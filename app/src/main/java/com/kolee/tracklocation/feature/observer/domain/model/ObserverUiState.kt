package com.kolee.tracklocation.feature.observer.domain.model

sealed class AllowlistScope {
    object AllPackages : AllowlistScope()
    data class FilteredCount(val active: Int, val total: Int) : AllowlistScope()
}

data class FilterState(
    val selectedPackages: Set<String> = emptySet(),
    val query: String = "",
) {
    val isActive: Boolean get() = selectedPackages.isNotEmpty() || query.isNotBlank()
}

data class ObserverUiState(
    val serviceEnabled: Boolean = false,
    val captureRunning: Boolean = true,
    val autoScrollRunning: Boolean = true,
    val events: List<ObservedEvent> = emptyList(),
    val totalEventCount: Int = 0,
    val scope: AllowlistScope = AllowlistScope.AllPackages,
    val showJumpToLatestFab: Boolean = false,
    val allowlistDraftPending: Boolean = false,
    val snackbarMessage: String? = null,
    val canLoadMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val filter: FilterState = FilterState(),
)

data class AllowlistDraftRule(
    val id: String,
    val matchType: MatchType,
    val pattern: String,
    val enabled: Boolean,
    val isDraft: Boolean = false,
)

data class AllowlistUiState(
    val rules: List<AllowlistDraftRule> = emptyList(),
    val hasDraftChanges: Boolean = false,
)
