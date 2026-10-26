package com.abit.multimedia.video

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel

/**
 * Remembers the video position and whether it should keep playing. The VideoView itself is
 * destroyed on rotation and when the user leaves the screen, so this state has to live here.
 * SavedStateHandle keeps it even if the system kills the app in the background.
 */
class VideoViewModel(private val savedState: SavedStateHandle) : ViewModel() {

    /** Where to continue, in milliseconds. */
    var positionMs: Int
        get() = savedState[KEY_POSITION_MS] ?: 0
        set(value) {
            savedState[KEY_POSITION_MS] = value
        }

    /** True when playback should resume as soon as the video is prepared (e.g. after rotation). */
    var playWhenReady: Boolean
        get() = savedState[KEY_PLAY_WHEN_READY] ?: false
        set(value) {
            savedState[KEY_PLAY_WHEN_READY] = value
        }

    private companion object {
        const val KEY_POSITION_MS = "video_position_ms"
        const val KEY_PLAY_WHEN_READY = "video_play_when_ready"
    }
}
