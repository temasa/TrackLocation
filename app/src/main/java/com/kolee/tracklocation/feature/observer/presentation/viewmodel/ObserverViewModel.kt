package com.kolee.tracklocation.feature.observer.presentation.viewmodel

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityManager
import com.kolee.tracklocation.observer.ObserverAccessibilityService
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kolee.tracklocation.TrackApp
import com.kolee.tracklocation.data.roomdb.AllowlistRuleEntity
import com.kolee.tracklocation.data.roomdb.TrackDatabase
import com.kolee.tracklocation.feature.observer.data.ObserverPreferencesDataStore
import com.kolee.tracklocation.feature.observer.data.repository.EventRepository
import com.kolee.tracklocation.feature.observer.data.repository.EventRepositoryImpl
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistDraftRule
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistScope
import com.kolee.tracklocation.feature.observer.domain.model.AllowlistUiState
import com.kolee.tracklocation.feature.observer.domain.model.MatchType
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import com.kolee.tracklocation.feature.observer.domain.model.ObserverUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ObserverViewModel(
    private val context: Context,
    private val prefs: ObserverPreferencesDataStore,
    private val eventRepository: EventRepository,
) : ViewModel() {

    private val allowlistRuleDao = TrackDatabase.getDatabase(context).allowlistRuleDao
    private val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager

    private val _uiState = MutableStateFlow(ObserverUiState())
    val uiState: StateFlow<ObserverUiState> = _uiState.asStateFlow()

    private val _allowlistUiState = MutableStateFlow(AllowlistUiState())
    val allowlistUiState: StateFlow<AllowlistUiState> = _allowlistUiState.asStateFlow()

    // IDs of rules deleted during the current draft session
    private val pendingDeleteIds = mutableSetOf<String>()

    init {
        // Poll service enabled state every second
        viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(serviceEnabled = isOurServiceEnabled()) }
                delay(1_000)
            }
        }

        // Observe capture state from DataStore
        viewModelScope.launch {
            prefs.captureRunning.collect { running ->
                _uiState.update { it.copy(captureRunning = running) }
            }
        }

        // Observe event stream
        viewModelScope.launch {
            eventRepository.getEvents().collect { events ->
                _uiState.update { it.copy(events = events, totalEventCount = events.size) }
            }
        }

        // Load applied allowlist rules once from DB (drafts are managed in ViewModel state)
        viewModelScope.launch {
            allowlistRuleDao.getAllRules().take(1).collect { entities ->
                val rules = entities.map { e ->
                    AllowlistDraftRule(
                        id = e.id,
                        matchType = if (e.matchType == "EXACT") MatchType.EXACT else MatchType.REGEX,
                        pattern = e.pattern,
                        enabled = e.enabled,
                        isDraft = false,
                    )
                }
                _allowlistUiState.update { it.copy(rules = rules) }
                refreshScope(rules)
            }
        }
    }

    fun toggleCapture() {
        viewModelScope.launch {
            prefs.setCaptureRunning(!_uiState.value.captureRunning)
        }
    }

    fun copyToClipboard(event: ObservedEvent) {
        val text = buildString {
            append(event.packageName)
            event.activityName?.let { append(" | ").append(it) }
        }
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        cm.setPrimaryClip(ClipData.newPlainText("Observer event", text))
        _uiState.update { it.copy(snackbarMessage = "Copied") }
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    // Allowlist draft management -----------------------------------------------

    fun addDraftRule() {
        val newRule = AllowlistDraftRule(
            id = UUID.randomUUID().toString(),
            matchType = MatchType.EXACT,
            pattern = "",
            enabled = true,
            isDraft = true,
        )
        val updated = listOf(newRule) + _allowlistUiState.value.rules
        _allowlistUiState.update { it.copy(rules = updated, hasDraftChanges = true) }
        _uiState.update { it.copy(allowlistDraftPending = true) }
    }

    fun updateDraftRule(rule: AllowlistDraftRule) {
        val updated = _allowlistUiState.value.rules.map {
            if (it.id == rule.id) rule.copy(isDraft = true) else it
        }
        _allowlistUiState.update { it.copy(rules = updated, hasDraftChanges = true) }
        _uiState.update { it.copy(allowlistDraftPending = true) }
    }

    fun deleteDraftRule(id: String) {
        pendingDeleteIds.add(id)
        val updated = _allowlistUiState.value.rules.filter { it.id != id }
        _allowlistUiState.update { it.copy(rules = updated, hasDraftChanges = true) }
        _uiState.update { it.copy(allowlistDraftPending = true) }
    }

    fun applyAllowlist() {
        viewModelScope.launch {
            // Delete rules removed in the draft
            pendingDeleteIds.forEach { allowlistRuleDao.deleteById(it) }
            pendingDeleteIds.clear()

            // Upsert all current rules
            val rules = _allowlistUiState.value.rules
            rules.forEach { rule ->
                allowlistRuleDao.insert(
                    AllowlistRuleEntity(
                        id = rule.id,
                        matchType = rule.matchType.name,
                        pattern = rule.pattern,
                        enabled = rule.enabled,
                        createdAt = System.currentTimeMillis(),
                    )
                )
            }

            val applied = rules.map { it.copy(isDraft = false) }
            _allowlistUiState.update { it.copy(rules = applied, hasDraftChanges = false) }
            _uiState.update { it.copy(allowlistDraftPending = false) }
            refreshScope(applied)

            _uiState.update { it.copy(snackbarMessage = "Allowlist updated") }
        }
    }

    private fun refreshScope(rules: List<AllowlistDraftRule>) {
        val enabled = rules.filter { it.enabled }
        val scope = if (enabled.isEmpty()) AllowlistScope.AllPackages
                    else AllowlistScope.FilteredCount(enabled.size, rules.size)
        _uiState.update { it.copy(scope = scope) }
    }

    private fun isOurServiceEnabled(): Boolean {
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == ObserverAccessibilityService::class.java.name
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as TrackApp
                ObserverViewModel(
                    context = app,
                    prefs = ObserverPreferencesDataStore(app),
                    eventRepository = EventRepositoryImpl(app),
                )
            }
        }
    }
}
