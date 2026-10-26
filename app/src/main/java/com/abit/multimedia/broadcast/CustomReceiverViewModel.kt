package com.abit.multimedia.broadcast

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/** A message that arrived through the custom broadcast. */
data class ReceivedBroadcast(
    val message: String,
    val receivedAtMillis: Long,
    /** 1 for the first broadcast, 2 after "Broadcast again", and so on. */
    val count: Int,
)

/**
 * Holds Activity 3's state. The ViewModel survives rotation, and SavedStateHandle also survives
 * the system killing the app in the background.
 */
class CustomReceiverViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    private val _receivedBroadcast = MutableLiveData(restoreReceivedBroadcast())

    /** The last message received through the broadcast, or null before the first one. */
    val receivedBroadcast: LiveData<ReceivedBroadcast?> = _receivedBroadcast

    /**
     * True once Activity 3 has broadcast the message from Activity 2. After a rotation this stays
     * true, so the broadcast is not sent again automatically.
     */
    var initialBroadcastSent: Boolean
        get() = savedState[KEY_INITIAL_SENT] ?: false
        set(value) {
            savedState[KEY_INITIAL_SENT] = value
        }

    fun onBroadcastReceived(message: String, receivedAtMillis: Long) {
        val count = (_receivedBroadcast.value?.count ?: 0) + 1
        savedState[KEY_MESSAGE] = message
        savedState[KEY_RECEIVED_AT] = receivedAtMillis
        savedState[KEY_COUNT] = count
        _receivedBroadcast.value = ReceivedBroadcast(message, receivedAtMillis, count)
    }

    private fun restoreReceivedBroadcast(): ReceivedBroadcast? {
        val message = savedState.get<String>(KEY_MESSAGE) ?: return null
        return ReceivedBroadcast(
            message = message,
            receivedAtMillis = savedState[KEY_RECEIVED_AT] ?: 0L,
            count = savedState[KEY_COUNT] ?: 1,
        )
    }

    private companion object {
        const val KEY_MESSAGE = "received_message"
        const val KEY_RECEIVED_AT = "received_at"
        const val KEY_COUNT = "received_count"
        const val KEY_INITIAL_SENT = "initial_broadcast_sent"
    }
}
