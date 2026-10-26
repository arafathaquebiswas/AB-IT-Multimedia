package com.abit.multimedia.broadcast

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receives our custom broadcast and hands the text to [onMessageReceived].
 *
 * It is a context-registered receiver: CustomReceiverActivity registers it at runtime with
 * ContextCompat.registerReceiver() and unregisters it again, so it is not in the manifest.
 */
class CustomMessageReceiver(
    private val onMessageReceived: (String) -> Unit,
) : BroadcastReceiver() {

    // Runs on the main thread, so it can call straight into the Activity.
    override fun onReceive(context: Context, intent: Intent) {
        val applicationId = context.packageName
        if (intent.action != CustomBroadcast.action(applicationId)) return

        val message = intent.getStringExtra(CustomBroadcast.messageExtra(applicationId))
        if (message == null) {
            Log.w(TAG, "Custom broadcast arrived without a message extra")
            return
        }
        Log.i(TAG, "Custom broadcast received: action=${intent.action}, message=\"$message\"")
        onMessageReceived(message)
    }

    private companion object {
        const val TAG = "CustomMessageReceiver"
    }
}
