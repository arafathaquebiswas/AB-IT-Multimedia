package com.abit.multimedia.broadcast.battery

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData

/**
 * LiveData that listens to the system ACTION_BATTERY_CHANGED broadcast, but only while someone
 * is observing it from a visible screen.
 *
 * LiveData calls onActive() when its first observer becomes STARTED (the screen is visible) and
 * onInactive() when the last one stops. Registering and unregistering the receiver there means
 * nothing listens to the battery while the screen is hidden.
 */
class BatteryStatusLiveData(context: Context) : LiveData<BatteryStatus>() {

    // The application context outlives any Activity, so holding it here cannot leak an Activity.
    private val appContext = context.applicationContext

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BATTERY_CHANGED) {
                value = BatteryStatus.fromIntent(intent)
            }
        }
    }

    override fun onActive() {
        // ACTION_BATTERY_CHANGED is a "sticky" broadcast: the system keeps the latest one, and
        // registerReceiver() returns it immediately. That lets us show the battery level straight
        // away instead of waiting for the next change.
        // The flag is required for runtime receivers on Android 14+; NOT_EXPORTED blocks other
        // apps but still lets the system's own broadcasts through.
        val stickyIntent = ContextCompat.registerReceiver(
            appContext,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        Log.d(TAG, "Battery receiver registered")
        if (stickyIntent != null) {
            value = BatteryStatus.fromIntent(stickyIntent)
        }
    }

    override fun onInactive() {
        appContext.unregisterReceiver(batteryReceiver)
        Log.d(TAG, "Battery receiver unregistered")
    }

    private companion object {
        const val TAG = "BatteryStatusLiveData"
    }
}
