package com.abit.multimedia.broadcast

/**
 * The two broadcast demos offered by the Spinner on the Broadcast Receiver screen.
 * The order matches the string-array R.array.broadcast_types.
 */
enum class BroadcastMode {
    /** "Custom broadcast receiver": the user's text is sent to our own CustomMessageReceiver. */
    CUSTOM,

    /** "System battery notification receiver": the system ACTION_BATTERY_CHANGED broadcast. */
    BATTERY;

    companion object {
        /** Intent extra that tells Activity 2 and Activity 3 which option was chosen. */
        const val EXTRA_MODE = "extra_broadcast_mode"

        /** The mode for a Spinner position, or null for an invalid position. */
        fun fromSpinnerPosition(position: Int): BroadcastMode? = entries.getOrNull(position)

        /**
         * Reads the mode back from the Intent extra, which holds the enum's name ("CUSTOM" or
         * "BATTERY"). A missing or unknown value gives null.
         */
        fun fromExtraValue(value: String?): BroadcastMode? = entries.firstOrNull { it.name == value }
    }
}
