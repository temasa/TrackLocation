package com.kolee.tracklocation.feature.observer.trip

/**
 * Per-app UI strings used to locate the order-card blocks. Kept as data (keyed by package name)
 * so a Gojek UI-copy change only touches this table, not the parser logic.
 */
data class OrderCardRules(
    val packageName: String,
    val dividerText: String,
    val paymentLabel: String,
    val earningsLabel: String,
    val pickupButton: String,
    val dropButton: String,
    val finishedButton: String,
    // ADR-015 terminal signals: bottom-nav labels that only co-occur on the home screen, and the
    // cancel dialog's button + message marker.
    val homeNavTexts: List<String> = emptyList(),
    val cancelButton: String? = null,
    val cancelMarker: String? = null,
    // ADR-025: payment text that marks a cash order (compared case-insensitively).
    val cashPaymentLabel: String? = null
)

object GojekRules {
    const val PACKAGE_NAME = "com.gojek.partner"

    val GOJEK = OrderCardRules(
        packageName = PACKAGE_NAME,
        dividerText = "Laporkan masalah map",
        paymentLabel = "Dibayar pakai",
        earningsLabel = "Pendapatan",
        pickupButton = "Udah di titik jemput",
        dropButton = "Sampai tujuan",
        finishedButton = "Selesai",
        homeNavTexts = listOf("Beranda", "Pendapatan", "Swadaya", "Pesan"),
        cancelButton = "Oke, sip",
        cancelMarker = "nge-cancel",
        cashPaymentLabel = "Tunai"
    )

    private val byPackage: Map<String, OrderCardRules> = mapOf(GOJEK.packageName to GOJEK)

    fun forPackage(packageName: String): OrderCardRules? = byPackage[packageName]
}
