package com.kolee.tracklocation.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kolee.tracklocation.feature.obd.service.ObdPollingService
import com.kolee.tracklocation.feature.obd.service.ObdUiState
import com.kolee.tracklocation.observer.isObserverServiceEnabled
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * State for the floating status banner (ADR-028). Activity-scoped so the collapsed flag
 * survives tab switches; it is in-memory only, so a process restart expands it again.
 */
class StatusBannerViewModel(private val app: Application) : ViewModel() {

    private val _observerProblem = MutableStateFlow(!isObserverServiceEnabled(app))
    val observerProblem: StateFlow<Boolean> = _observerProblem.asStateFlow()

    private val _obdProblem = MutableStateFlow(false)
    val obdProblem: StateFlow<Boolean> = _obdProblem.asStateFlow()

    private val _collapsed = MutableStateFlow(false)
    val collapsed: StateFlow<Boolean> = _collapsed.asStateFlow()

    private var pollJob: Job? = null

    init {
        viewModelScope.launch {
            ObdPollingService.obdUiState.collect { state ->
                setObdProblem(state !is ObdUiState.Connected)
            }
        }
    }

    fun setCollapsed(value: Boolean) {
        _collapsed.value = value
    }

    // A chip whose problem newly appears re-expands the banner, so a warning is never hidden.
    private fun setObdProblem(problem: Boolean) {
        if (problem && !_obdProblem.value) _collapsed.value = false
        _obdProblem.value = problem
    }

    private fun setObserverProblem(problem: Boolean) {
        if (problem && !_observerProblem.value) _collapsed.value = false
        _observerProblem.value = problem
    }

    /** Polls the accessibility state every second; call from ON_START, stop on ON_STOP. */
    fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = viewModelScope.launch {
            while (true) {
                setObserverProblem(!isObserverServiceEnabled(app))
                delay(1_000)
            }
        }
    }

    fun stopPolling() {
        pollJob?.cancel()
        pollJob = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                StatusBannerViewModel(app)
            }
        }
    }
}
