package com.abit.multimedia.audio

import android.app.Application
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abit.multimedia.R
import com.abit.multimedia.media.PlaybackCoordinator
import com.abit.multimedia.media.PlaybackOwner
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** Everything the Audio screen displays. */
data class AudioUiState(
    val isPrepared: Boolean = false,
    val isPlaying: Boolean = false,
    val positionMs: Int = 0,
    val durationMs: Int = 0,
    val hasError: Boolean = false,
)

/**
 * Owns the android.media.MediaPlayer that plays res/raw/sample_audio.mp3.
 *
 * Keeping the player in a ViewModel instead of the Fragment means it survives rotation, so the
 * music doesn't restart. It is released in onCleared(), when the screen is gone for good.
 *
 * MediaPlayer states used here:
 *   Idle -> setDataSource() -> Initialized -> prepareAsync() -> Preparing -> Prepared
 *   Prepared/Paused -> start() -> Started -> pause() -> Paused
 *   Started/Paused -> stop() -> Stopped (must be prepared again before the next start())
 *   end of track -> PlaybackCompleted;  release() -> End
 */
class AudioPlayerViewModel(application: Application) : AndroidViewModel(application), PlaybackOwner {

    private val _uiState = MutableStateFlow(AudioUiState())
    val uiState: StateFlow<AudioUiState> = _uiState.asStateFlow()

    private var player: MediaPlayer? = null
    private var isPrepared = false
    private var playWhenPrepared = false
    private var progressJob: Job? = null

    init {
        createPlayer()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) pause() else play()
    }

    fun play() {
        val mediaPlayer = player ?: return
        if (!isPrepared) {
            playWhenPrepared = true // onPrepared() will start playback
            return
        }
        // Pauses the video first if it is playing.
        PlaybackCoordinator.onPlaybackStarted(this)
        mediaPlayer.start()
        _uiState.update { it.copy(isPlaying = true) }
        startProgressUpdates()
    }

    fun pause() {
        playWhenPrepared = false
        val mediaPlayer = player ?: return
        if (isPrepared && mediaPlayer.isPlaying) mediaPlayer.pause()
        stopProgressUpdates()
        _uiState.update { it.copy(isPlaying = false, positionMs = currentPosition()) }
        PlaybackCoordinator.onPlaybackStopped(this)
    }

    fun stop() {
        playWhenPrepared = false
        val mediaPlayer = player ?: return
        stopProgressUpdates()
        if (isPrepared) {
            mediaPlayer.stop()
            // A stopped MediaPlayer has to be prepared again before it can play.
            isPrepared = false
            mediaPlayer.prepareAsync()
        }
        _uiState.update { it.copy(isPrepared = false, isPlaying = false, positionMs = 0) }
        PlaybackCoordinator.onPlaybackStopped(this)
    }

    fun seekTo(positionMs: Int) {
        val mediaPlayer = player ?: return
        if (!isPrepared) return
        mediaPlayer.seekTo(positionMs)
        _uiState.update { it.copy(positionMs = positionMs) }
    }

    /** Called by the PlaybackCoordinator when the video starts. */
    override fun pausePlayback() = pause()

    /** The Audio screen is gone for good: free the native player. */
    override fun onCleared() {
        stopProgressUpdates()
        PlaybackCoordinator.onPlaybackStopped(this)
        player?.release()
        player = null
        isPrepared = false
    }

    private fun createPlayer() {
        val mediaPlayer = MediaPlayer()
        mediaPlayer.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
        )
        mediaPlayer.setOnPreparedListener { onPrepared(it) }
        mediaPlayer.setOnCompletionListener { onPlaybackCompleted() }
        mediaPlayer.setOnErrorListener { _, what, extra -> onPlayerError(what, extra) }
        try {
            getApplication<Application>().resources.openRawResourceFd(R.raw.sample_audio)
                .use { file -> mediaPlayer.setDataSource(file) }
        } catch (e: Exception) {
            // The file is missing or unreadable.
            Log.e(TAG, "Could not open the audio file", e)
            mediaPlayer.release()
            _uiState.value = AudioUiState(hasError = true)
            return
        }
        player = mediaPlayer
        // Prepare without blocking the main thread; onPrepared() is called when ready.
        mediaPlayer.prepareAsync()
    }

    private fun onPrepared(mediaPlayer: MediaPlayer) {
        isPrepared = true
        _uiState.update {
            it.copy(isPrepared = true, durationMs = mediaPlayer.duration, hasError = false)
        }
        if (playWhenPrepared) {
            playWhenPrepared = false
            play()
        }
    }

    private fun onPlaybackCompleted() {
        stopProgressUpdates()
        player?.seekTo(0) // the next Play starts from the beginning
        _uiState.update { it.copy(isPlaying = false, positionMs = 0) }
        PlaybackCoordinator.onPlaybackStopped(this)
    }

    private fun onPlayerError(what: Int, extra: Int): Boolean {
        Log.e(TAG, "MediaPlayer error: what=$what extra=$extra")
        isPrepared = false
        playWhenPrepared = false
        stopProgressUpdates()
        _uiState.update { it.copy(isPrepared = false, isPlaying = false, hasError = true) }
        PlaybackCoordinator.onPlaybackStopped(this)
        // true = handled; otherwise MediaPlayer would also call the completion listener.
        return true
    }

    /**
     * Updates the position four times a second while playing. The coroutine runs in
     * viewModelScope, so it is cancelled automatically when the ViewModel is cleared.
     */
    private fun startProgressUpdates() {
        progressJob?.cancel()
        progressJob = viewModelScope.launch {
            while (isActive) {
                _uiState.update { it.copy(positionMs = currentPosition()) }
                delay(PROGRESS_UPDATE_INTERVAL_MS)
            }
        }
    }

    private fun stopProgressUpdates() {
        progressJob?.cancel()
        progressJob = null
    }

    private fun currentPosition(): Int = if (isPrepared) player?.currentPosition ?: 0 else 0

    private companion object {
        const val TAG = "AudioPlayerViewModel"
        const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }
}
