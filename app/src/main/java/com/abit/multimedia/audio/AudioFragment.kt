package com.abit.multimedia.audio

import android.os.Bundle
import android.view.View
import android.widget.SeekBar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.abit.multimedia.R
import com.abit.multimedia.databinding.FragmentAudioBinding
import kotlinx.coroutines.launch

/**
 * D. Audio: Play/Pause, Stop and a seek bar for the bundled MP3.
 * The MediaPlayer itself lives in [AudioPlayerViewModel]; this Fragment only shows its state and
 * forwards button presses.
 */
class AudioFragment : Fragment(R.layout.fragment_audio) {

    private var _binding: FragmentAudioBinding? = null

    // Only valid between onViewCreated() and onDestroyView().
    private val binding get() = _binding!!

    private val viewModel: AudioPlayerViewModel by viewModels()

    /** True while the user drags the seek bar, so progress updates don't fight the finger. */
    private var isUserSeeking = false

    private val seekBarListener = object : SeekBar.OnSeekBarChangeListener {
        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            if (!fromUser) return
            // Show the time under the finger while dragging.
            _binding?.elapsedTimeText?.text = formatPlaybackTime(progress)
            // Keyboard and screen-reader adjustments change the value without a drag.
            if (!isUserSeeking) viewModel.seekTo(progress)
        }

        override fun onStartTrackingTouch(seekBar: SeekBar) {
            isUserSeeking = true
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            isUserSeeking = false
            viewModel.seekTo(seekBar.progress)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAudioBinding.bind(view)

        binding.playPauseButton.setOnClickListener { viewModel.togglePlayPause() }
        binding.stopButton.setOnClickListener { viewModel.stop() }
        binding.audioSeekBar.setOnSeekBarChangeListener(seekBarListener)

        // Collect the state only while the screen is visible (STARTED). repeatOnLifecycle stops
        // collecting in onStop() and starts again in onStart(), so no work is done in the background.
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state -> render(state) }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Pause when this screen is no longer visible (another drawer screen, or the app went to
        // the background). During a rotation the screen comes straight back, so keep playing.
        if (!requireActivity().isChangingConfigurations) viewModel.pause()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun render(state: AudioUiState) {
        val binding = _binding ?: return
        binding.playPauseButton.setIconResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play)
        binding.playPauseButton.contentDescription =
            getString(if (state.isPlaying) R.string.audio_pause else R.string.audio_play)
        binding.playPauseButton.isEnabled = !state.hasError
        binding.stopButton.isEnabled = state.isPrepared

        binding.audioSeekBar.isEnabled = state.isPrepared
        binding.audioSeekBar.max = state.durationMs
        if (!isUserSeeking) {
            binding.audioSeekBar.progress = state.positionMs
            binding.elapsedTimeText.text = formatPlaybackTime(state.positionMs)
        }
        binding.totalTimeText.text = formatPlaybackTime(state.durationMs)
        binding.audioErrorText.isVisible = state.hasError
    }
}
