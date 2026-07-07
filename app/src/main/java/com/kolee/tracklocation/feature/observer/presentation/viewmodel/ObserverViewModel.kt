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
import com.kolee.tracklocation.feature.observer.domain.model.FilterState
import com.kolee.tracklocation.feature.observer.domain.model.MatchType
import com.kolee.tracklocation.feature.observer.domain.model.ObservedEvent
import com.kolee.tracklocation.feature.observer.domain.model.ObserverUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
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

    // Emits the number of items prepended by loadMore() so the feed can adjust scroll position
    private val _prependedCount = MutableSharedFlow<Int>(extraBufferCapacity = 1)
    val prependedCount: SharedFlow<Int> = _prependedCount

    // In-memory paginated event list
    private val _loadedEvents = mutableListOf<ObservedEvent>()
    private val _loadedIds = mutableSetOf<Long>()
    private var _oldestLastSeenAt = Long.MAX_VALUE
    private var _newestLastSeenAt = 0L

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

        // Load first page then subscribe to live new events
        viewModelScope.launch {
            val firstPage = loadFilteredFirstPage(_uiState.value.filter)
            _loadedEvents.addAll(firstPage)
            _loadedIds.addAll(firstPage.map { it.id })
            if (firstPage.isNotEmpty()) {
                _oldestLastSeenAt = firstPage.first().timestampMs
                _newestLastSeenAt = firstPage.last().timestampMs
            }
            _uiState.update { it.copy(
                events = _loadedEvents.toList(),
                totalEventCount = _loadedEvents.size,
                canLoadMore = firstPage.size >= PAGE_SIZE,
            )}

            // Subscribe only to events newer than what was just loaded
            eventRepository.getEventsNewerThan(_newestLastSeenAt).collect { newerEvents ->
                val filter = _uiState.value.filter
                val newOnes = newerEvents
                    .filter { it.id !in _loadedIds }
                    .filter { !filter.isActive || matchesFilter(it, filter) }
                if (newOnes.isNotEmpty()) {
                    _loadedEvents.addAll(newOnes)
                    _loadedIds.addAll(newOnes.map { it.id })
                    _newestLastSeenAt = newOnes.last().timestampMs
                    _uiState.update { it.copy(
                        events = _loadedEvents.toList(),
                        totalEventCount = _loadedEvents.size,
                    )}
                }
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

    fun loadMore() {
        if (!_uiState.value.canLoadMore || _uiState.value.isLoadingMore) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }
            val older = loadFilteredNextPage(_uiState.value.filter, _oldestLastSeenAt)
            val newOnes = older.filter { it.id !in _loadedIds }
            if (newOnes.isNotEmpty()) {
                _loadedIds.addAll(newOnes.map { it.id })
                val combined = newOnes + _loadedEvents.toList()
                _loadedEvents.clear()
                _loadedEvents.addAll(combined)
                _oldestLastSeenAt = newOnes.first().timestampMs
                _prependedCount.tryEmit(newOnes.size)
            }
            _uiState.update { it.copy(
                events = _loadedEvents.toList(),
                totalEventCount = _loadedEvents.size,
                canLoadMore = older.size >= PAGE_SIZE,
                isLoadingMore = false,
            )}
        }
    }

    // Filtering ----------------------------------------------------------------

    fun setQuery(q: String) {
        _uiState.update { it.copy(filter = it.filter.copy(query = q)) }
        reloadForFilter()
    }

    fun togglePackage(pkg: String) {
        _uiState.update {
            val current = it.filter.selectedPackages
            val updated = if (pkg in current) current - pkg else current + pkg
            it.copy(filter = it.filter.copy(selectedPackages = updated))
        }
        reloadForFilter()
    }

    fun clearFilter() {
        _uiState.update { it.copy(filter = FilterState()) }
        reloadForFilter()
    }

    private fun reloadForFilter() {
        viewModelScope.launch {
            _loadedEvents.clear()
            _loadedIds.clear()
            _oldestLastSeenAt = Long.MAX_VALUE
            _newestLastSeenAt = 0L

            val firstPage = loadFilteredFirstPage(_uiState.value.filter)
            _loadedEvents.addAll(firstPage)
            _loadedIds.addAll(firstPage.map { it.id })
            if (firstPage.isNotEmpty()) {
                _oldestLastSeenAt = firstPage.first().timestampMs
                _newestLastSeenAt = firstPage.last().timestampMs
            }
            _uiState.update { it.copy(
                events = _loadedEvents.toList(),
                totalEventCount = _loadedEvents.size,
                canLoadMore = firstPage.size >= PAGE_SIZE,
            )}
        }
    }

    // Routes the first-page load through the active filter (none / query / packages / both).
    private suspend fun loadFilteredFirstPage(filter: FilterState): List<ObservedEvent> {
        val pkgs = filter.selectedPackages.toList()
        val fts = formatFtsQuery(filter.query)
        return when {
            pkgs.isNotEmpty() && fts != null ->
                eventRepository.getPackageTextFirstPage(fts, pkgs, PAGE_SIZE)
            pkgs.isNotEmpty() ->
                eventRepository.getPackageFirstPage(pkgs, PAGE_SIZE)
            fts != null ->
                eventRepository.getFilteredFirstPage(fts, PAGE_SIZE)
            else ->
                eventRepository.getFirstPage(PAGE_SIZE)
        }
    }

    // Routes the next-page (older) load through the active filter.
    private suspend fun loadFilteredNextPage(filter: FilterState, beforeLastSeenAt: Long): List<ObservedEvent> {
        val pkgs = filter.selectedPackages.toList()
        val fts = formatFtsQuery(filter.query)
        return when {
            pkgs.isNotEmpty() && fts != null ->
                eventRepository.getPackageTextNextPage(fts, pkgs, beforeLastSeenAt, PAGE_SIZE)
            pkgs.isNotEmpty() ->
                eventRepository.getPackageNextPage(pkgs, beforeLastSeenAt, PAGE_SIZE)
            fts != null ->
                eventRepository.getFilteredNextPage(fts, beforeLastSeenAt, PAGE_SIZE)
            else ->
                eventRepository.getNextPage(beforeLastSeenAt, PAGE_SIZE)
        }
    }

    // Formats free user text into an FTS4 MATCH expression: trim, strip double-quotes,
    // split on whitespace, append '*' to each token for prefix matching. Returns null when blank.
    private fun formatFtsQuery(raw: String): String? {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return null
        val tokens = trimmed
            .replace("\"", "")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .map { "$it*" }
        if (tokens.isEmpty()) return null
        return tokens.joinToString(" ")
    }

    // In-memory match used by the live tail when a filter is active.
    private fun matchesFilter(event: ObservedEvent, filter: FilterState): Boolean {
        val packageOk = filter.selectedPackages.isEmpty() ||
            event.packageName in filter.selectedPackages
        val queryOk = filter.query.isBlank() ||
            listOfNotNull(event.packageName, event.activityName, event.textSummary)
                .any { it.contains(filter.query, ignoreCase = true) }
        return packageOk && queryOk
    }

    private fun isOurServiceEnabled(): Boolean {
        val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        return enabled.any {
            it.resolveInfo.serviceInfo.packageName == context.packageName &&
            it.resolveInfo.serviceInfo.name == ObserverAccessibilityService::class.java.name
        }
    }

    companion object {
        private const val PAGE_SIZE = 50

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
