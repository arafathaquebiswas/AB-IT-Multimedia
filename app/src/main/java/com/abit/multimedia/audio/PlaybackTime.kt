package com.abit.multimedia.audio

import java.util.Locale

/** Formats a playback position in milliseconds as "mm:ss", for example 83_000 -> "01:23". */
fun formatPlaybackTime(millis: Int): String {
    val totalSeconds = millis.coerceAtLeast(0) / 1000
    return String.format(Locale.US, "%02d:%02d", totalSeconds / 60, totalSeconds % 60)
}
