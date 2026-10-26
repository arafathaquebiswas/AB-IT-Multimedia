package com.abit.multimedia.image

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import androidx.appcompat.widget.AppCompatImageView

/**
 * An ImageView that can be zoomed with two fingers.
 *
 *  - Pinch: ScaleGestureDetector reports how far the fingers moved apart (scaleFactor) and the
 *    point between them (focusX/focusY). The image is scaled around that point.
 *  - One-finger drag while zoomed: GestureDetector.onScroll() moves the image.
 *  - Double-tap: zoom in to 2.5x at the tapped point, or back to 1x when already zoomed.
 *
 * Everything is done with a Matrix that the ImageView draws the image with (scaleType MATRIX).
 * "1x" means the whole image fits inside the view; the zoom is limited to 1x..5x.
 */
class ZoomableImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    /** Called with the new zoom level whenever it changes, e.g. to show "2.4x". */
    var onZoomChanged: ((Float) -> Unit)? = null

    /** Current zoom relative to the fitted size, always between [MIN_ZOOM] and [MAX_ZOOM]. */
    var zoom = MIN_ZOOM
        private set

    // Scales and centres the image so that all of it fits inside the view (this is 1x).
    private val fitMatrix = Matrix()

    // The user's zoom and pan, applied on top of fitMatrix.
    private val userMatrix = Matrix()

    // fitMatrix followed by userMatrix: what the image is finally drawn with.
    private val drawMatrix = Matrix()

    private val imageBounds = RectF()
    private var zoomAnimator: ValueAnimator? = null

    private val scaleDetector = ScaleGestureDetector(context, PinchListener())
    private val gestureDetector = GestureDetector(context, TapAndDragListener())

    // The ImageView constructor can call setImageDrawable() before the properties above exist.
    // This flag lets us ignore that early call.
    private var isReady = false

    private val contentWidth get() = (width - paddingLeft - paddingRight).toFloat()
    private val contentHeight get() = (height - paddingTop - paddingBottom).toFloat()

    init {
        scaleType = ScaleType.MATRIX
        isReady = true
    }

    // Coil calls this when the downloaded image is ready (and with null while loading).
    override fun setImageDrawable(drawable: Drawable?) {
        super.setImageDrawable(drawable)
        if (isReady) resetZoom()
    }

    // First layout, rotation, window resize...: the fit has to be recalculated.
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        resetZoom()
    }

    // Single taps are passed on to performClick() by the GestureDetector (see onSingleTapConfirmed).
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (drawable == null) return super.onTouchEvent(event)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) zoomAnimator?.cancel()

        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)

        // While zoomed or pinching, stop parents (such as the DrawerLayout) from taking over the
        // gesture, so dragging the image near the screen edge doesn't open the drawer.
        parent?.requestDisallowInterceptTouchEvent(zoom > MIN_ZOOM || scaleDetector.isInProgress)
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    override fun onDetachedFromWindow() {
        zoomAnimator?.cancel()
        super.onDetachedFromWindow()
    }

    /** Fits the image inside the view again (1x) and forgets any zoom or pan. */
    fun resetZoom() {
        zoomAnimator?.cancel()
        userMatrix.reset()
        zoom = MIN_ZOOM
        updateFitMatrix()
        applyMatrix()
    }

    private fun updateFitMatrix() {
        fitMatrix.reset()
        val image = drawable ?: return
        val imageWidth = image.intrinsicWidth.toFloat()
        val imageHeight = image.intrinsicHeight.toFloat()
        if (imageWidth <= 0f || imageHeight <= 0f || contentWidth <= 0f || contentHeight <= 0f) return

        fitMatrix.setRectToRect(
            RectF(0f, 0f, imageWidth, imageHeight),
            RectF(0f, 0f, contentWidth, contentHeight),
            Matrix.ScaleToFit.CENTER,
        )
    }

    /** Multiplies the zoom by [factor] around the point ([focusX], [focusY]) of the view. */
    private fun zoomBy(factor: Float, focusX: Float, focusY: Float) {
        val newZoom = (zoom * factor).coerceIn(MIN_ZOOM, MAX_ZOOM)
        val appliedFactor = newZoom / zoom
        // Touch coordinates include the view's padding; the image matrix doesn't.
        userMatrix.postScale(appliedFactor, appliedFactor, focusX - paddingLeft, focusY - paddingTop)
        zoom = newZoom
        keepImageInsideView()
        applyMatrix()
    }

    private fun panBy(dx: Float, dy: Float) {
        userMatrix.postTranslate(dx, dy)
        keepImageInsideView()
        applyMatrix()
    }

    /**
     * Stops the image from being dragged away: on an axis where it is smaller than the view it is
     * centred, otherwise its edges can't move past the edges of the view.
     */
    private fun keepImageInsideView() {
        val image = drawable ?: return
        drawMatrix.set(fitMatrix)
        drawMatrix.postConcat(userMatrix)
        imageBounds.set(0f, 0f, image.intrinsicWidth.toFloat(), image.intrinsicHeight.toFloat())
        drawMatrix.mapRect(imageBounds)

        val dx = correctionFor(imageBounds.left, imageBounds.right, contentWidth)
        val dy = correctionFor(imageBounds.top, imageBounds.bottom, contentHeight)
        userMatrix.postTranslate(dx, dy)
    }

    /** How far one axis of the image must move so it stays inside a view of size [viewSize]. */
    private fun correctionFor(start: Float, end: Float, viewSize: Float): Float {
        val imageSize = end - start
        return when {
            imageSize <= viewSize -> (viewSize - imageSize) / 2f - start // centre it
            start > 0f -> -start // gap at the left/top edge
            end < viewSize -> viewSize - end // gap at the right/bottom edge
            else -> 0f
        }
    }

    private fun applyMatrix() {
        drawMatrix.set(fitMatrix)
        drawMatrix.postConcat(userMatrix)
        imageMatrix = drawMatrix // ImageView copies the matrix and redraws
        onZoomChanged?.invoke(zoom)
    }

    private fun animateZoomTo(targetZoom: Float, focusX: Float, focusY: Float) {
        zoomAnimator?.cancel()
        zoomAnimator = ValueAnimator.ofFloat(zoom, targetZoom).apply {
            duration = ZOOM_ANIMATION_DURATION_MS
            addUpdateListener { animator ->
                val animatedZoom = animator.animatedValue as Float
                zoomBy(animatedZoom / zoom, focusX, focusY)
            }
            start()
        }
    }

    private inner class PinchListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            zoomBy(detector.scaleFactor, detector.focusX, detector.focusY)
            return true
        }
    }

    private inner class TapAndDragListener : GestureDetector.SimpleOnGestureListener() {
        // Returning true for ACTION_DOWN is required to receive the rest of the gesture.
        override fun onDown(e: MotionEvent): Boolean = true

        override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
            if (zoom <= MIN_ZOOM || scaleDetector.isInProgress) return false
            // distanceX/Y are "previous minus current" positions, so move the opposite way.
            panBy(-distanceX, -distanceY)
            return true
        }

        override fun onDoubleTap(e: MotionEvent): Boolean {
            val target = if (zoom > MIN_ZOOM) MIN_ZOOM else DOUBLE_TAP_ZOOM
            animateZoomTo(target, e.x, e.y)
            return true
        }

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean = performClick()
    }

    private companion object {
        const val MIN_ZOOM = 1f
        const val MAX_ZOOM = 5f
        const val DOUBLE_TAP_ZOOM = 2.5f
        const val ZOOM_ANIMATION_DURATION_MS = 250L
    }
}
