package com.kolee.tracklocation.feature.observer.trip

/**
 * Pure parser: ordered snapshot texts -> [OrderCard], or null when the texts are not a (complete)
 * order card. No Android imports; UI strings come from [OrderCardRules].
 */
object OrderCardParser {

    fun parse(texts: List<String>, rules: OrderCardRules): OrderCard? {
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
