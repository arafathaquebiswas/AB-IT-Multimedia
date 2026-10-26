package com.abit.multimedia.audio

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackTimeTest {

    @Test
    fun formatsMillisecondsAsMinutesAndSeconds() {
        assertEquals("00:00", formatPlaybackTime(0))
        assertEquals("00:01", formatPlaybackTime(1_000))
        assertEquals("01:23", formatPlaybackTime(83_000))
        assertEquals("03:25", formatPlaybackTime(205_139)) // length of the bundled track
    }

    @Test
    fun partialSecondsAreDropped() {
        assertEquals("00:00", formatPlaybackTime(999))
        assertEquals("00:59", formatPlaybackTime(59_999))
    }

    @Test
    fun minutesKeepCountingPastAnHour() {
        assertEquals("60:00", formatPlaybackTime(3_600_000))
    }

    @Test
    fun negativeValuesShowZero() {
        assertEquals("00:00", formatPlaybackTime(-500))
    }
}
