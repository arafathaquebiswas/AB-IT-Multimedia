package com.abit.multimedia.broadcast

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import androidx.core.widget.TextViewCompat
import com.abit.multimedia.R
import com.abit.multimedia.databinding.ActivityCustomReceiverBinding
import com.abit.multimedia.ui.applySystemBarInsetsAsPadding
import com.google.android.material.color.MaterialColors
import java.text.DateFormat
import java.util.Date

/**
 * A. Activity 3 of the broadcast flow.
 *
 * CUSTOM option:
 *  1. Activity 2 gave us the user's text in an Intent extra ([EXTRA_MESSAGE]).
 *  2. onStart() registers [CustomMessageReceiver] for our custom action.
 *  3. Only then is the text sent as a broadcast that is restricted to this app's package.
 *  4. The receiver gets it in onReceive() and the screen shows "Received via broadcast: <text>".
 *
 * Why does Activity 3 send the broadcast, not Activity 2? A runtime-registered receiver only
 * hears broadcasts sent while it is registered. If Activity 2 sent the broadcast before
 * Activity 3 had registered its receiver, the message could simply be missed.
 *
 * BATTERY option: the battery broadcast was already handled in Activity 2, so this screen
 * registers nothing and only shows a short note.
 */
class CustomReceiverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCustomReceiverBinding
    private val viewModel: CustomReceiverViewModel by viewModels()
    private val timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM)

    private var mode: BroadcastMode? = null
    private var messageToBroadcast: String? = null

    /** The registered receiver, or null while no receiver is registered. */
    private var customMessageReceiver: CustomMessageReceiver? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCustomReceiverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applySystemBarInsetsAsPadding(applyTop = true)
        binding.content.applySystemBarInsetsAsPadding(applyBottom = true)
        // Up arrow: close this Activity and return to Activity 2.
        binding.toolbar.setNavigationOnClickListener { finish() }

        mode = BroadcastMode.fromExtraValue(intent.getStringExtra(BroadcastMode.EXTRA_MODE))
        when (mode) {
            BroadcastMode.CUSTOM -> {
                messageToBroadcast = intent.getStringExtra(EXTRA_MESSAGE)
                if (messageToBroadcast.isNullOrBlank()) {
                    Log.w(TAG, "Started in custom mode without a message, closing")
                    finish()
                    return
                }
                showCustomReceiverScreen()
            }
            BroadcastMode.BATTERY -> showBatteryNote()
            null -> {
                Log.w(TAG, "Started without a valid broadcast mode, closing")
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Battery option: no custom receiver at all.
        if (mode != BroadcastMode.CUSTOM) return

        // 1) Register first...
        registerCustomMessageReceiver()
        // 2) ...then send. The message from Activity 2 is sent only once: after a rotation the
        // ViewModel remembers that it was already sent, so it isn't broadcast again by itself.
        if (!viewModel.initialBroadcastSent) {
            viewModel.initialBroadcastSent = true
            sendCustomBroadcast()
        }
    }

    override fun onStop() {
        // Always unregister what onStart() registered, otherwise the receiver would leak.
        unregisterCustomMessageReceiver()
        super.onStop()
    }

    private fun showCustomReceiverScreen() {
        binding.toolbar.setTitle(R.string.title_custom_receiver)
        binding.customReceiverGroup.isVisible = true
        binding.receiverActionText.text =
            getString(R.string.receiver_action_format, CustomBroadcast.action(packageName))
        binding.broadcastAgainButton.setOnClickListener { sendCustomBroadcast() }
        viewModel.receivedBroadcast.observe(this) { received -> showReceivedBroadcast(received) }
        showReceiverStatus()
    }

    private fun showBatteryNote() {
        binding.toolbar.setTitle(R.string.title_battery_receiver)
        binding.batteryNoteCard.isVisible = true
    }

    private fun registerCustomMessageReceiver() {
        if (customMessageReceiver != null) return
        val receiver = CustomMessageReceiver { message -> onCustomMessageReceived(message) }
        // RECEIVER_NOT_EXPORTED: broadcasts from other apps can't reach this receiver.
        ContextCompat.registerReceiver(
            this,
            receiver,
            IntentFilter(CustomBroadcast.action(packageName)),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        customMessageReceiver = receiver
        Log.d(TAG, "CustomMessageReceiver registered for ${CustomBroadcast.action(packageName)}")
        showReceiverStatus()
    }

    private fun unregisterCustomMessageReceiver() {
        val receiver = customMessageReceiver ?: return
        unregisterReceiver(receiver)
        customMessageReceiver = null
        Log.d(TAG, "CustomMessageReceiver unregistered")
        showReceiverStatus()
    }

    private fun sendCustomBroadcast() {
        val message = messageToBroadcast ?: return
        // An explicit, package-restricted broadcast (see CustomBroadcast.createIntent()).
        sendBroadcast(CustomBroadcast.createIntent(this, message))
        Log.d(TAG, "Sent custom broadcast: \"$message\"")
    }

    private fun onCustomMessageReceived(message: String) {
        viewModel.onBroadcastReceived(message, System.currentTimeMillis())
        Toast.makeText(this, getString(R.string.toast_broadcast_received, message), Toast.LENGTH_SHORT)
            .show()
    }

    private fun showReceiverStatus() {
        val registered = customMessageReceiver != null
        val statusText = binding.receiverStatusText
        statusText.setText(if (registered) R.string.receiver_registered else R.string.receiver_not_registered)
        statusText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            if (registered) R.drawable.ic_check_circle else R.drawable.ic_error_outline, 0, 0, 0,
        )
        val iconColor = MaterialColors.getColor(
            statusText,
            if (registered) androidx.appcompat.R.attr.colorPrimary else androidx.appcompat.R.attr.colorError,
        )
        TextViewCompat.setCompoundDrawableTintList(statusText, ColorStateList.valueOf(iconColor))
    }

    private fun showReceivedBroadcast(received: ReceivedBroadcast?) {
        if (received == null) {
            binding.receivedMessageText.setText(R.string.received_waiting)
            binding.receivedTimeText.isVisible = false
            return
        }
        binding.receivedMessageText.text = getString(R.string.received_message_format, received.message)
        binding.receivedTimeText.text = getString(
            R.string.received_time_format,
            timeFormat.format(Date(received.receivedAtMillis)),
            received.count,
        )
        binding.receivedTimeText.isVisible = true
    }

    companion object {
        private const val TAG = "CustomReceiverActivity"

        /** Intent extra with the user's text, passed from Activity 2. */
        const val EXTRA_MESSAGE = "extra_message"

        /** Explicit Intent that opens this Activity. [message] is only used in CUSTOM mode. */
        fun newIntent(context: Context, mode: BroadcastMode, message: String? = null): Intent {
            val intent = Intent(context, CustomReceiverActivity::class.java)
                .putExtra(BroadcastMode.EXTRA_MODE, mode.name)
            if (message != null) intent.putExtra(EXTRA_MESSAGE, message)
            return intent
        }
    }
}
