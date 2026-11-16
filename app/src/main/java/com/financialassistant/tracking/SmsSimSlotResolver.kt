package com.financialassistant.tracking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.telephony.SubscriptionInfo
import android.telephony.SubscriptionManager
import androidx.core.content.ContextCompat
import timber.log.Timber

/**
 * Best-effort resolution of the SIM slot (0 = SIM 1, 1 = SIM 2) that an incoming SMS
 * arrived on for a dual-SIM device.
 *
 * Android does not expose this uniformly: AOSP broadcasts the subscription id via the
 * `subscription` extra, while some OEMs provide a slot index directly. When neither is
 * available [SLOT_UNKNOWN] is returned.
 */
object SmsSimSlotResolver {

    const val SLOT_UNKNOWN = -1

    private val SLOT_EXTRA_KEYS = arrayOf("slot", "simSlot", "SimSlot", "slotId", "slot_id")
    private val SUBSCRIPTION_EXTRA_KEYS =
        arrayOf("subscription", "subscriptionId", "SubscriptionId", "sub_id")

    /**
     * Resolves the receiving SIM slot for an SMS_RECEIVED broadcast intent.
     */
    fun resolveSlot(context: Context, intent: Intent): Int {
        // 1. OEM-provided slot extras.
        for (key in SLOT_EXTRA_KEYS) {
            val slot = intent.getIntExtra(key, SLOT_UNKNOWN)
            if (slot in 0..1) return slot
        }

        // 2. AOSP subscription id extra, mapped to a slot index.
        var subscriptionId = SLOT_UNKNOWN
        for (key in SUBSCRIPTION_EXTRA_KEYS) {
            val value = intent.getIntExtra(key, SLOT_UNKNOWN)
            if (value != SLOT_UNKNOWN) {
                subscriptionId = value
                break
            }
        }
        if (subscriptionId != SLOT_UNKNOWN) {
            slotFromSubscriptionId(context, subscriptionId)?.let { return it }
        }

        return SLOT_UNKNOWN
    }

    /** Maps a subscription id to its SIM slot index, or `null` when unavailable. */
    fun slotFromSubscriptionId(context: Context, subscriptionId: Int): Int? {
        if (subscriptionId == SLOT_UNKNOWN) return null
        if (!hasPhoneStatePermission(context)) return null
        return try {
            val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            manager?.getActiveSubscriptionInfo(subscriptionId)?.simSlotIndex
        } catch (e: SecurityException) {
            Timber.w(e, "Missing permission to resolve SIM slot for subscription $subscriptionId")
            null
        } catch (e: Exception) {
            Timber.w(e, "Failed to resolve SIM slot for subscription $subscriptionId")
            null
        }
    }

    /** Returns the active subscriptions, or an empty list when permission is missing. */
    fun activeSubscriptions(context: Context): List<SubscriptionInfo> {
        if (!hasPhoneStatePermission(context)) return emptyList()
        return try {
            val manager = context.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as? SubscriptionManager
            manager?.activeSubscriptionInfoList ?: emptyList()
        } catch (e: SecurityException) {
            Timber.w(e, "Missing permission to read active subscriptions")
            emptyList()
        } catch (e: Exception) {
            Timber.w(e, "Failed to read active subscriptions")
            emptyList()
        }
    }

    /**
     * Returns the mobile lines currently present on the device, sorted by SIM slot.
     * Empty when phone permission is missing or no active subscription is readable.
     */
    fun activeLines(context: Context): List<SimLine> =
        activeSubscriptions(context)
            .filter { it.simSlotIndex >= 0 }
            .sortedBy { it.simSlotIndex }
            .map { info ->
                SimLine(
                    subscriptionId = info.subscriptionId,
                    slot = info.simSlotIndex,
                    number = subscriptionNumber(info)?.let { MsisdnNormalizer.normalize(it) },
                    displayName = info.displayName?.toString().orEmpty(),
                    carrierName = info.carrierName?.toString().orEmpty()
                )
            }

    /**
     * Attempts to find the SIM slot whose number matches [canonicalNumber]
     * (a value produced by [MsisdnNormalizer.normalize]). Returns `null` when it
     * cannot be determined, e.g. permission denied or the number is not stored on the SIM.
     */
    fun findSlotForNumber(context: Context, canonicalNumber: String?): Int? {
        if (canonicalNumber == null) return null
        return activeSubscriptions(context).firstOrNull { info ->
            MsisdnNormalizer.normalize(subscriptionNumber(info)) == canonicalNumber
        }?.simSlotIndex
    }

    fun subscriptionNumber(info: SubscriptionInfo): String? {
        return try {
            info.number
        } catch (e: SecurityException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    fun hasPhoneStatePermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) ==
            PackageManager.PERMISSION_GRANTED
}
