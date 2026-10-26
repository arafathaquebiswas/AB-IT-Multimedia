package com.abit.multimedia.video

import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.MediaController
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.abit.multimedia.R
import com.abit.multimedia.databinding.FragmentVideoBinding
import com.abit.multimedia.media.PlaybackCoordinator
import com.abit.multimedia.media.PlaybackOwner

/**
 * C. Video: plays the bundled res/raw/sample_video.mp4 in a VideoView with a MediaController.
 *
 * Lifecycle:
 *  - onStart(): open the video. When it is prepared, seek to the saved position (and resume
 *    playing if it was playing before a rotation).
 *  - onStop(): remember the position, then pause, so nothing plays behind another screen.
 *  - onDestroyView(): hide the MediaController and call stopPlayback() to free the player.
 */
class VideoFragment : Fragment(R.layout.fragment_video) {

    private var _binding: FragmentVideoBinding? = null

    // Only valid between onViewCreated() and onDestroyView().
    private val binding get() = _binding!!

    private val viewModel: VideoViewModel by viewModels()
    private var mediaController: MediaController? = null

    /** True once the current video is prepared and can report its position. */
    private var isPrepared = false

    /** Lets the PlaybackCoordinator pause the video when the audio player starts. */
    private val playbackOwner = PlaybackOwner { _binding?.videoView?.pause() }

    /**
     * The MediaController floats in its own window above the app, so it would stay on top of the
     * navigation drawer. Hide it as soon as the drawer starts to slide open.
     */
    private val hideControlsWhenDrawerMoves = object : DrawerLayout.SimpleDrawerListener() {
        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            mediaController?.hide()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentVideoBinding.bind(view)

        val controller = MediaController(requireContext())
        mediaController = controller
        // VideoView anchors the controller to its parent (the black video container) and shows
        // or hides it when the video is tapped.
        binding.videoView.setMediaController(controller)
        binding.videoView.onBeforeStart = { PlaybackCoordinator.onPlaybackStarted(playbackOwner) }
        binding.videoView.setOnPreparedListener { onVideoPrepared() }
        binding.videoView.setOnErrorListener { _, what, extra -> onVideoError(what, extra) }
        binding.videoView.setOnCompletionListener { onVideoCompleted() }
        binding.videoRetryButton.setOnClickListener { openVideo() }

        findDrawerLayout()?.addDrawerListener(hideControlsWhenDrawerMoves)
    }

    override fun onStart() {
        super.onStart()
        openVideo()
    }

    override fun onStop() {
        super.onStop()
        val videoView = binding.videoView
        if (isPrepared) {
            viewModel.positionMs = videoView.currentPosition
            // Keep playing after a rotation, but not after the user left the screen or the app.
            viewModel.playWhenReady = videoView.isPlaying && requireActivity().isChangingConfigurations
        }
        // Never keep playing behind another screen or while the app is in the background.
        videoView.pause()
        PlaybackCoordinator.onPlaybackStopped(playbackOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        findDrawerLayout()?.removeDrawerListener(hideControlsWhenDrawerMoves)
        // The controller's window must be removed before the Activity window goes away.
        mediaController?.hide()
        mediaController = null
        binding.videoView.onBeforeStart = null
        binding.videoView.stopPlayback() // releases the MediaPlayer inside the VideoView
        PlaybackCoordinator.onPlaybackStopped(playbackOwner)
        _binding = null
    }

    private fun openVideo() {
        isPrepared = false
        binding.videoLoadingIndicator.isVisible = true
        binding.videoErrorGroup.isVisible = false
        val videoUri = "android.resource://${requireContext().packageName}/${R.raw.sample_video}".toUri()
        // Preparing happens asynchronously; onVideoPrepared() runs when the video is ready.
        binding.videoView.setVideoURI(videoUri)
    }

    private fun onVideoPrepared() {
        val binding = _binding ?: return
        isPrepared = true
        binding.videoLoadingIndicator.isVisible = false
        // Continue where the user left off (0 the first time).
        binding.videoView.seekTo(viewModel.positionMs)
        if (viewModel.playWhenReady) {
            binding.videoView.start()
        } else {
            // Paused: keep the controls on screen so the Play button is visible.
            mediaController?.show(0)
        }
    }

    private fun onVideoError(what: Int, extra: Int): Boolean {
        Log.e(TAG, "Video playback error: what=$what extra=$extra")
        isPrepared = false
        mediaController?.hide()
        PlaybackCoordinator.onPlaybackStopped(playbackOwner)
        _binding?.let { binding ->
            binding.videoLoadingIndicator.isVisible = false
            binding.videoErrorGroup.isVisible = true
        }
        // true = we handled the error, so Android does not show its own error dialog.
        return true
    }

    private fun onVideoCompleted() {
        viewModel.positionMs = 0
        viewModel.playWhenReady = false
        PlaybackCoordinator.onPlaybackStopped(playbackOwner)
        mediaController?.show(0)
    }

    // The drawer belongs to MainActivity; null if this Fragment is ever hosted elsewhere.
    private fun findDrawerLayout(): DrawerLayout? = activity?.findViewById(R.id.drawer_layout)

    private companion object {
        const val TAG = "VideoFragment"
    }
}
