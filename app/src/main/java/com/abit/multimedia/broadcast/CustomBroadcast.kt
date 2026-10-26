package com.abit.multimedia.broadcast

import android.content.Context
import android.content.Intent

/**
 * The names used by our custom broadcast. They are built from the application ID at runtime
 * (Context.packageName), for example "com.abit.multimedia.action.CUSTOM_MESSAGE", so they are
 * unique to this app.
 */
object CustomBroadcast {

    /** The action CustomMessageReceiver listens for. */
    fun action(applicationId: String): String = "$applicationId.action.CUSTOM_MESSAGE"

    /** The Intent extra that carries the text. */
    fun messageExtra(applicationId: String): String = "$applicationId.extra.MESSAGE"

    /**
     * Builds the broadcast sent by Activity 3. setPackage() limits delivery to this app,
     * so no other installed app can receive the message.
     */
    fun createIntent(context: Context, message: String): Intent {
        val applicationId = context.packageName
        return Intent(action(applicationId))
            .setPackage(applicationId)
            .putExtra(messageExtra(applicationId), message)
    }
}
