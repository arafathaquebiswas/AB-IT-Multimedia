package com.abit.multimedia.media

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class PlaybackCoordinatorTest {

    private class FakePlayer : PlaybackOwner {
        var pauseCount = 0

        override fun pausePlayback() {
            pauseCount++
        }
    }

    private val audio = FakePlayer()
    private val video = FakePlayer()

    @After
    fun tearDown() {
        PlaybackCoordinator.onPlaybackStopped(audio)
        PlaybackCoordinator.onPlaybackStopped(video)
    }

    @Test
    fun startingVideoPausesPlayingAudio() {
        PlaybackCoordinator.onPlaybackStarted(audio)
        PlaybackCoordinator.onPlaybackStarted(video)

        assertEquals(1, audio.pauseCount)
        assertEquals(0, video.pauseCount)
        assertSame(video, PlaybackCoordinator.activeOwner)
    }

    @Test
    fun startingAudioPausesPlayingVideo() {
        PlaybackCoordinator.onPlaybackStarted(video)
        PlaybackCoordinator.onPlaybackStarted(audio)

        assertEquals(1, video.pauseCount)
        assertSame(audio, PlaybackCoordinator.activeOwner)
    }

    @Test
    fun startingTheSamePlayerAgainDoesNotPauseIt() {
        PlaybackCoordinator.onPlaybackStarted(audio)
        PlaybackCoordinator.onPlaybackStarted(audio)

        assertEquals(0, audio.pauseCount)
    }

    @Test
    fun playerThatAlreadyStoppedIsNotPausedAgain() {
        PlaybackCoordinator.onPlaybackStarted(audio)
        PlaybackCoordinator.onPlaybackStopped(audio)
        assertNull(PlaybackCoordinator.activeOwner)

        PlaybackCoordinator.onPlaybackStarted(video)
        assertEquals(0, audio.pauseCount)
    }

    @Test
    fun stoppingAPlayerThatIsNotActiveKeepsTheActiveOne() {
        PlaybackCoordinator.onPlaybackStarted(video)
        PlaybackCoordinator.onPlaybackStopped(audio)

        assertSame(video, PlaybackCoordinator.activeOwner)
    }

    @Test
    fun ownerThatReportsStoppedWhilePausedDoesNotClearTheNewOwner() {
        // Like AudioPlayerViewModel: pausePlayback() itself calls onPlaybackStopped().
        val selfReportingAudio = object : PlaybackOwner {
            override fun pausePlayback() = PlaybackCoordinator.onPlaybackStopped(this)
        }
        PlaybackCoordinator.onPlaybackStarted(selfReportingAudio)
        PlaybackCoordinator.onPlaybackStarted(video)

        assertSame(video, PlaybackCoordinator.activeOwner)
    }
}
