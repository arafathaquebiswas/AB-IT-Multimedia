package com.abit.multimedia

import android.graphics.Matrix
import android.graphics.RectF
import android.os.SystemClock
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.doubleClick
import androidx.test.espresso.action.ViewActions.swipeLeft
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.abit.multimedia.image.ZoomableImageView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Image Scale gestures on the real ZoomableImageView. Needs internet access, because the photo
 * is downloaded from picsum.photos.
 */
@RunWith(AndroidJUnit4::class)
class ImageScaleTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Before
    fun openImageScaleAndWaitForThePhoto() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.nav_view)).perform(NavigationViewActions.navigateTo(R.id.nav_image_scale))
        waitUntilDisplayed(allOf(withId(R.id.zoom_level_text), withText("1.0x")), timeoutMs = 30_000)
    }

    @Test
    fun pinchOutZoomsInAroundTheFingersAndStopsAt5x() {
        onView(withId(R.id.zoomable_image)).perform(pinch(startGapPx = 500f, endGapPx = 1000f))
        val zoomAfterOnePinch = currentZoom()
        assertTrue("zoom should grow, was $zoomAfterOnePinch", zoomAfterOnePinch > 1.5f)
        onView(withId(R.id.zoom_level_text)).check { view, _ ->
            assertEquals("%.1fx".format(zoomAfterOnePinch), (view as android.widget.TextView).text.toString())
        }

        // Two more 2x pinches would be 8x, but the zoom is limited to 5x.
        repeat(2) { onView(withId(R.id.zoomable_image)).perform(pinch(startGapPx = 500f, endGapPx = 1000f)) }
        assertEquals(5f, currentZoom(), 0.01f)
    }

    @Test
    fun pinchInCannotZoomOutBelowTheFittedSize() {
        onView(withId(R.id.zoomable_image)).perform(pinch(startGapPx = 1000f, endGapPx = 500f))
        assertEquals(1f, currentZoom(), 0.01f)
    }

    @Test
    fun doubleTapZoomsInAndDoubleTapAgainResets() {
        onView(withId(R.id.zoomable_image)).perform(doubleClick())
        retryUntilPasses { assertEquals(2.5f, currentZoom(), 0.01f) }

        onView(withId(R.id.zoomable_image)).perform(doubleClick())
        retryUntilPasses { assertEquals(1f, currentZoom(), 0.01f) }
        waitUntilDisplayed(allOf(withId(R.id.zoom_level_text), withText("1.0x")))
    }

    @Test
    fun dragMovesTheZoomedImageButKeepsItInsideTheView() {
        onView(withId(R.id.zoomable_image)).perform(doubleClick())
        retryUntilPasses { assertEquals(2.5f, currentZoom(), 0.01f) }
        val before = imageBounds()

        onView(withId(R.id.zoomable_image)).perform(swipeLeft())

        val after = imageBounds()
        assertNotEquals("the image should have moved", before.left, after.left, 1f)
        val viewWidth = viewWidth()
        // Zoomed wider than the view: its edges may not be dragged inside the view.
        assertTrue("left edge ${after.left} must stay <= 0", after.left <= 0.5f)
        assertTrue("right edge ${after.right} must stay >= $viewWidth", after.right >= viewWidth - 0.5f)
    }

    private fun currentZoom(): Float {
        var zoom = 0f
        activityRule.scenario.onActivity { activity ->
            zoom = activity.findViewById<ZoomableImageView>(R.id.zoomable_image).zoom
        }
        return zoom
    }

    /** Where the photo is drawn inside the view, worked out from the image matrix. */
    private fun imageBounds(): RectF {
        val bounds = RectF()
        activityRule.scenario.onActivity { activity ->
            val view = activity.findViewById<ZoomableImageView>(R.id.zoomable_image)
            val drawable = view.drawable
            bounds.set(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
            Matrix(view.imageMatrix).mapRect(bounds)
        }
        return bounds
    }

    private fun viewWidth(): Float {
        var width = 0f
        activityRule.scenario.onActivity { activity ->
            width = activity.findViewById<View>(R.id.zoomable_image).width.toFloat()
        }
        return width
    }

    /**
     * Two fingers placed across the centre of the view that move apart (zoom in) or together
     * (zoom out), sent as real multi-touch MotionEvents.
     */
    private fun pinch(startGapPx: Float, endGapPx: Float): ViewAction = object : ViewAction {
        override fun getConstraints(): Matcher<View> = isDisplayed()

        override fun getDescription() = "pinch from $startGapPx px to $endGapPx px"

        override fun perform(uiController: UiController, view: View) {
            val location = IntArray(2)
            view.getLocationOnScreen(location)
            val centerX = location[0] + view.width / 2f
            val centerY = location[1] + view.height / 2f
            val downTime = SystemClock.uptimeMillis()
            var eventTime = downTime

            fun inject(action: Int, gap: Float, pointerCount: Int) {
                val properties = Array(pointerCount) { index ->
                    MotionEvent.PointerProperties().apply {
                        id = index
                        toolType = MotionEvent.TOOL_TYPE_FINGER
                    }
                }
                val coordinates = Array(pointerCount) { index ->
                    MotionEvent.PointerCoords().apply {
                        x = centerX + if (index == 0) -gap / 2 else gap / 2
                        y = centerY
                        pressure = 1f
                        size = 1f
                    }
                }
                val event = MotionEvent.obtain(
                    downTime, eventTime, action, pointerCount, properties, coordinates,
                    0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_TOUCHSCREEN, 0,
                )
                uiController.injectMotionEvent(event)
                event.recycle()
                eventTime += 10
                uiController.loopMainThreadForAtLeast(10)
            }

            val secondPointer = 1 shl MotionEvent.ACTION_POINTER_INDEX_SHIFT
            inject(MotionEvent.ACTION_DOWN, startGapPx, pointerCount = 1)
            inject(MotionEvent.ACTION_POINTER_DOWN or secondPointer, startGapPx, pointerCount = 2)
            val steps = 20
            for (step in 1..steps) {
                inject(MotionEvent.ACTION_MOVE, startGapPx + (endGapPx - startGapPx) * step / steps, pointerCount = 2)
            }
            inject(MotionEvent.ACTION_POINTER_UP or secondPointer, endGapPx, pointerCount = 2)
            inject(MotionEvent.ACTION_UP, endGapPx, pointerCount = 1)
        }
    }
}
