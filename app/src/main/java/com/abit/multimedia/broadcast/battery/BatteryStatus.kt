package com.abit.multimedia.broadcast.battery

import android.content.Intent
import android.os.BatteryManager
import androidx.annotation.StringRes
import com.abit.multimedia.R

/** What the battery screen shows from one ACTION_BATTERY_CHANGED broadcast. */
data class BatteryStatus(
    /** Charge from 0 to 100, or null when the broadcast had no valid level/scale. */
    val percentage: Int?,
    val chargingStatus: ChargingStatus,
    /** When this status was received, from System.currentTimeMillis(). */
    val updatedAtMillis: Long,
) {
    companion object {
        /**
         * The broadcast reports the charge as EXTRA_LEVEL out of EXTRA_SCALE (often, but not
         * always, out of 100), so the percentage is level * 100 / scale.
         * Returns null for a missing level (-1) or a scale <= 0, which would otherwise give a
         * wrong value or a division by zero.
         */
        fun percentageOf(level: Int, scale: Int): Int? {
            if (level < 0 || scale <= 0) return null
            return (level * 100 / scale).coerceIn(0, 100)
        }

        /** Reads the extras of an ACTION_BATTERY_CHANGED Intent. */
        fun fromIntent(intent: Intent, receivedAtMillis: Long = System.currentTimeMillis()): BatteryStatus {
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN,
            )
            return BatteryStatus(
                percentage = percentageOf(level, scale),
                chargingStatus = ChargingStatus.fromExtraStatus(status),
                updatedAtMillis = receivedAtMillis,
            )
        }
    }
}

/** The charging state from BatteryManager.EXTRA_STATUS, with the text shown for it. */
enum class ChargingStatus(@param:StringRes @get:StringRes val labelRes: Int) {
    CHARGING(R.string.battery_status_charging),
    DISCHARGING(R.string.battery_status_discharging),
    FULL(R.string.battery_status_full),
    NOT_CHARGING(R.string.battery_status_not_charging),
    UNKNOWN(R.string.battery_status_unknown);

    companion object {
        fun fromExtraStatus(status: Int): ChargingStatus = when (status) {
            BatteryManager.BATTERY_STATUS_CHARGING -> CHARGING
            BatteryManager.BATTERY_STATUS_DISCHARGING -> DISCHARGING
            BatteryManager.BATTERY_STATUS_FULL -> FULL
            BatteryManager.BATTERY_STATUS_NOT_CHARGING -> NOT_CHARGING
            else -> UNKNOWN
        }
    }
}
