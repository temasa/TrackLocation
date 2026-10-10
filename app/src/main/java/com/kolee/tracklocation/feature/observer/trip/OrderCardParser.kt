package com.kolee.tracklocation.feature.observer.trip

/**
 * Pure parser: ordered snapshot texts -> [OrderCard], or null when the texts are not a (complete)
 * order card. No Android imports; UI strings come from [OrderCardRules].
 *
 * ADR-015 order lifecycle (Gojek terms): Taken = [OrderPhase.PICKUP] card, Carrying =
 * [OrderPhase.DROP] card, Drop off = [OrderPhase.FINISHED] card ("Selesai"). Two further terminal
 * signals yield a bare card: the cancel message (Cancelled, [OrderPhase.CANCELLED], ADR-025) and
 * the driver home screen (Cleared, [OrderPhase.FINISHED]).
 */
object OrderCardParser {

    fun parse(texts: List<String>, rules: OrderCardRules): OrderCard? {
        // Cancelled: checked first because the cancel message can co-occur with the order card's
        // "Udah di titik jemput" button, which would otherwise be read as PICKUP.
        val cancelButton = rules.cancelButton
        val cancelMarker = rules.cancelMarker
        if (cancelButton != null && texts.contains(cancelButton) &&
            cancelMarker != null && texts.any { it.contains(cancelMarker, ignoreCase = true) }
        ) {
            // ADR-025: distinct phase so cash-order cancels can keep the trip running.
            return OrderCard(phase = OrderPhase.CANCELLED)
        }
        // Cleared: the home screen (all bottom-nav labels present) means no order is being served.
        if (rules.homeNavTexts.isNotEmpty() && texts.containsAll(rules.homeNavTexts)) {
            return OrderCard(phase = OrderPhase.FINISHED)
        }

        val phase = when {
            texts.contains(rules.pickupButton) -> OrderPhase.PICKUP
            texts.contains(rules.dropButton) -> OrderPhase.DROP
            texts.contains(rules.finishedButton) -> OrderPhase.FINISHED
            else -> return null
        }

        val paymentIdx = texts.indexOf(rules.paymentLabel)
        val payment = if (paymentIdx >= 0) texts.getOrNull(paymentIdx + 1) else null
        val earningsIdx = texts.indexOf(rules.earningsLabel)
        val earnings = if (earningsIdx >= 0) parseRupiah(texts.getOrNull(earningsIdx + 1)) else null

        if (phase == OrderPhase.FINISHED) {
            return OrderCard(phase = phase, payment = payment, earningsRp = earnings)
        }

        val divider = texts.indexOf(rules.dividerText)
        if (divider < 2 || paymentIdx <= divider) return null
        val beforeName = texts[divider - 2]
        val beforeAddress = texts[divider - 1]

        return if (phase == OrderPhase.PICKUP) {
            // after-divider block (name, address) must fit before the payment label
            if (paymentIdx < divider + 3) return null
            OrderCard(
                phase = phase,
                pickupName = beforeName,
                pickupAddress = beforeAddress,
                dropName = texts[divider + 1],
                dropAddress = texts[divider + 2],
                payment = payment,
                earningsRp = earnings
            )
        } else {
            OrderCard(
                phase = phase,
                dropName = beforeName,
                dropAddress = beforeAddress,
                payment = payment,
                earningsRp = earnings
            )
        }
    }

    /** "Rp36.400" -> 36400 ('.' is the thousands separator). Null when no digits. */
    internal fun parseRupiah(raw: String?): Long? {
        val digits = raw?.filter { it.isDigit() } ?: return null
        return digits.takeIf { it.isNotEmpty() }?.toLongOrNull()
    }
}
