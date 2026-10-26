package com.abit.multimedia.broadcast

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewGroupCompat
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import com.abit.multimedia.R
import com.abit.multimedia.broadcast.battery.BatteryStatus
import com.abit.multimedia.broadcast.battery.BatteryViewModel
import com.abit.multimedia.databinding.ActivityBroadcastInputBinding
import com.abit.multimedia.ui.applySystemBarInsetsAsPadding
import java.text.DateFormat
import java.util.Date

/**
 * A. Activity 2 of the broadcast flow. What it shows depends on the option chosen on the first
 * screen:
 *  - CUSTOM: a text field. The text is passed on to Activity 3 in an Intent extra.
 *  - BATTERY: the live battery level from the system ACTION_BATTERY_CHANGED broadcast.
 */
class BroadcastInputActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBroadcastInputBinding

    // Created on first use, so it only exists (and only listens to the battery) in BATTERY mode.
    private val batteryViewModel: BatteryViewModel by viewModels()

    private val timeFormat = DateFormat.getTimeInstance(DateFormat.MEDIUM)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityBroadcastInputBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewGroupCompat.installCompatInsetsDispatch(binding.root)
        binding.appBar.applySystemBarInsetsAsPadding(applyTop = true)
        binding.content.applySystemBarInsetsAsPadding(applyBottom = true, includeKeyboard = true)
        // Up arrow: close this Activity and return to the previous screen.
        binding.toolbar.setNavigationOnClickListener { finish() }

        when (BroadcastMode.fromExtraValue(intent.getStringExtra(BroadcastMode.EXTRA_MODE))) {
            BroadcastMode.CUSTOM -> showCustomMessageForm()
            BroadcastMode.BATTERY -> showBatteryStatus()
            null -> {
                Log.w(TAG, "Started without a valid broadcast mode, closing")
                finish()
            }
        }
    }

    // ---- Option 1: custom broadcast receiver -------------------------------------------------

    private fun showCustomMessageForm() {
        binding.toolbar.setTitle(R.string.title_custom_broadcast)
        val form = binding.customInput
        form.root.isVisible = true

        form.messageEditText.doAfterTextChanged { form.messageInputLayout.error = null }
        // The keyboard's Send key does the same as the button.
        form.messageEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                openActivity3WithMessage()
                true
            } else {
                false
            }
        }
        form.sendButton.setOnClickListener { openActivity3WithMessage() }
    }

    private fun openActivity3WithMessage() {
        val form = binding.customInput
        val message = form.messageEditText.text?.toString()?.trim().orEmpty()
        if (message.isEmpty()) {
            form.messageInputLayout.error = getString(R.string.error_message_blank)
            return
        }
        // Pass the text to Activity 3 in an Intent extra. Activity 3 broadcasts it only after
        // registering its receiver (CustomReceiverActivity explains why).
        startActivity(CustomReceiverActivity.newIntent(this, BroadcastMode.CUSTOM, message))
    }

    // ---- Option 2: system battery notification receiver --------------------------------------

    private fun showBatteryStatus() {
        binding.toolbar.setTitle(R.string.title_battery_status)
        binding.batteryStatus.root.isVisible = true
        // Observing with this Activity as the lifecycle owner means the battery receiver is only
        // registered while this screen is visible, and is unregistered when it stops.
        batteryViewModel.batteryStatus.observe(this) { status -> showBattery(status) }
        binding.batteryStatus.nextButton.setOnClickListener {
            startActivity(CustomReceiverActivity.newIntent(this, BroadcastMode.BATTERY))
        }
    }

    private fun showBattery(status: BatteryStatus) {
        val battery = binding.batteryStatus
        val percentage = status.percentage
        if (percentage != null) {
            battery.batteryPercentText.text = getString(R.string.battery_percent_format, percentage)
            battery.batteryProgress.setProgressCompat(percentage, true)
        } else {
            battery.batteryPercentText.setText(R.string.battery_percent_unknown)
            battery.batteryProgress.setProgressCompat(0, false)
        }
        battery.chargingStatusText.text = getString(
            R.string.battery_charging_status_format,
            getString(status.chargingStatus.labelRes),
        )
        battery.batteryUpdatedText.text = getString(
            R.string.battery_updated_format,
            timeFormat.format(Date(status.updatedAtMillis)),
        )
    }

    companion object {
        private const val TAG = "BroadcastInputActivity"

        /** Explicit Intent that opens this Activity for the chosen [mode]. */
        fun newIntent(context: Context, mode: BroadcastMode): Intent =
            Intent(context, BroadcastInputActivity::class.java)
                .putExtra(BroadcastMode.EXTRA_MODE, mode.name)
    }
}
