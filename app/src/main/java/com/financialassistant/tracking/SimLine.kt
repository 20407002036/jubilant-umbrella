package com.financialassistant.tracking

/**
 * A mobile line that is currently present on the device (one per active SIM/eSIM
 * subscription). [slot] is the device SIM slot index, which is what transaction rows
 * are tagged with when an SMS arrives on that line.
 */
data class SimLine(
    val subscriptionId: Int,
    val slot: Int,
    val number: String?,
    val displayName: String,
    val carrierName: String
) {
    /** "SIM 1", "SIM 2", ... */
    val slotLabel: String get() = "SIM ${slot + 1}"

    /** The phone number when it is readable, otherwise a carrier/label fallback. */
    val numberLabel: String
        get() = number?.let { MsisdnNormalizer.format(it) }
            ?: displayName.takeIf { it.isNotBlank() }
            ?: carrierName.takeIf { it.isNotBlank() }
            ?: "No number"

    /** Secondary text used under [slotLabel] in the line toggle. */
    val subtitle: String
        get() = numberLabel
}
