package com.abit.multimedia.video

import android.media.MediaPlayer
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
     * navigation drawer. Hide it while the drawer opens, and bring it back when the drawer closes
     * over a paused video.
     */
    private val hideControlsWhenDrawerMoves = object : DrawerLayout.SimpleDrawerListener() {
        private var lastSlideOffset = 0f

        override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
            // Only while opening: the drawer also slides closed right after a drawer item is
            // picked, which must not hide the controls of the screen being opened.
            if (slideOffset > lastSlideOffset) mediaController?.hide()
            lastSlideOffset = slideOffset
        }

        override fun onDrawerClosed(drawerView: View) {
            lastSlideOffset = 0f
            val videoView = _binding?.videoView ?: return
            if (isPrepared && !videoView.isPlaying) mediaController?.show()
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
        binding.videoView.setOnPreparedListener { player -> onVideoPrepared(player) }
        binding.videoView.setOnErrorListener { _, what, extra -> onVideoError(what, extra) }
        binding.videoView.setOnCompletionListener { onVideoCompleted() }
        binding.videoRetryButton.setOnClickListener { openVideo() }

        findDrawerLayout()?.addDrawerListener(hideControlsWhenDrawerMoves)
    }

    override fun onStart() {
        super.onStart()
        openVideo()
    }

    override fun onPause() {
        super.onPause()
        // Save already here: when the app goes to the background, the video's surface can be
        // destroyed (and VideoView releases its player) before onStop() runs.
        rememberPlaybackState()
    }

    override fun onStop() {
        super.onStop()
        rememberPlaybackState()
        // Never keep playing behind another screen or while the app is in the background.
        binding.videoView.pause()
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
        isPrepared = false
        PlaybackCoordinator.onPlaybackStopped(playbackOwner)
        _binding = null
    }

    private fun rememberPlaybackState() {
        val videoView = _binding?.videoView ?: return
        // getDuration() is -1 once VideoView has released its player; the position would then
        // wrongly read 0, so keep the value saved earlier.
        if (!isPrepared || videoView.duration <= 0) return
        viewModel.positionMs = videoView.currentPosition
        // Keep playing after a rotation, but not after the user left the screen or the app.
        viewModel.playWhenReady = videoView.isPlaying && requireActivity().isChangingConfigurations
        Log.d(TAG, "Saved position ${viewModel.positionMs} ms (resume playing: ${viewModel.playWhenReady})")
    }

    private fun openVideo() {
        isPrepared = false
        binding.videoLoadingIndicator.isVisible = true
        binding.videoErrorGroup.isVisible = false
        val videoUri = "android.resource://${requireContext().packageName}/${R.raw.sample_video}".toUri()
        // Preparing happens asynchronously; onVideoPrepared() runs when the video is ready.
        binding.videoView.setVideoURI(videoUri)
    }

    private fun onVideoPrepared(player: MediaPlayer) {
        val binding = _binding ?: return
        isPrepared = true
        binding.videoLoadingIndicator.isVisible = false
        // While paused, show the controls so the user sees the Play button. Seeking is
        // asynchronous, so show them once the seek is done; then they show the right time.
        // show() hides them again after 3 seconds: while visible, MediaController's window takes
        // every touch, so leaving it up would block the toolbar and the drawer.
        player.setOnSeekCompleteListener {
            val videoView = _binding?.videoView ?: return@setOnSeekCompleteListener
            if (!videoView.isPlaying) mediaController?.show()
        }
        // Continue where the user left off (0 the first time).
        binding.videoView.seekTo(viewModel.positionMs)
        if (viewModel.playWhenReady) binding.videoView.start()
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
        mediaController?.show()
    }

    // The drawer belongs to MainActivity; null if this Fragment is ever hosted elsewhere.
    private fun findDrawerLayout(): DrawerLayout? = activity?.findViewById(R.id.drawer_layout)

    private companion object {
        const val TAG = "VideoFragment"
    }
}
