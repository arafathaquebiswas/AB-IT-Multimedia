package com.abit.multimedia.image

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import coil3.load
import coil3.size.Size
import com.abit.multimedia.R
import com.abit.multimedia.databinding.FragmentImageScaleBinding

/**
 * B. Image Scale: loads a photo from the internet with Coil and shows it in [ZoomableImageView],
 * which handles pinch-to-zoom, panning and double-tap.
 */
class ImageScaleFragment : Fragment(R.layout.fragment_image_scale) {

    private var _binding: FragmentImageScaleBinding? = null

    // Only valid between onViewCreated() and onDestroyView().
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentImageScaleBinding.bind(view)

        binding.zoomableImage.onZoomChanged = { zoom -> showZoomLevel(zoom) }
        binding.imageRetryButton.setOnClickListener { loadImage() }
        loadImage()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.zoomableImage.onZoomChanged = null
        _binding = null
    }

    private fun loadImage() {
        // Coil downloads and decodes the image off the main thread, caches it, and cancels the
        // request automatically if this screen's view is destroyed first.
        binding.zoomableImage.load(IMAGE_URL) {
            // Keep the photo's full 2000 x 1300 resolution instead of shrinking it to the view's
            // size, so it stays sharp when zoomed in.
            size(Size.ORIGINAL)
            listener(
                onStart = { showLoading() },
                onSuccess = { _, _ -> showImage() },
                onError = { _, result ->
                    Log.w(TAG, "Could not load $IMAGE_URL", result.throwable)
                    showError()
                },
            )
        }
    }

    private fun showLoading() {
        val binding = _binding ?: return
        binding.imageLoadingIndicator.isVisible = true
        binding.imageErrorGroup.isVisible = false
        binding.zoomLevelText.isVisible = false
    }

    private fun showImage() {
        val binding = _binding ?: return
        binding.imageLoadingIndicator.isVisible = false
        binding.imageErrorGroup.isVisible = false
        binding.zoomLevelText.isVisible = true
        showZoomLevel(binding.zoomableImage.zoom)
    }

    private fun showError() {
        val binding = _binding ?: return
        binding.imageLoadingIndicator.isVisible = false
        binding.imageErrorGroup.isVisible = true
        binding.zoomLevelText.isVisible = false
    }

    private fun showZoomLevel(zoom: Float) {
        _binding?.zoomLevelText?.text = getString(R.string.zoom_level_format, zoom)
    }

    companion object {
        private const val TAG = "ImageScaleFragment"

        /** Photo 1015 from Lorem Picsum (Unsplash), served at 2000 x 1300 pixels. */
        const val IMAGE_URL = "https://picsum.photos/id/1015/2000/1300"
    }
}
