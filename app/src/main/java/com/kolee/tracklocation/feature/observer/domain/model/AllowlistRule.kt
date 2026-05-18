package com.kolee.tracklocation.feature.observer.domain.model

enum class MatchType { EXACT, REGEX }

data class AllowlistRule(
    val id: String,
    val matchType: MatchType,
    val pattern: String,
    val enabled: Boolean
)
