package com.abit.multimedia

import android.view.Gravity
import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.Espresso.pressBack
import androidx.test.espresso.Root
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.DrawerMatchers.isClosed
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.matcher.RootMatchers.withDecorView
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withContentDescription
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.material.navigation.NavigationView
import org.hamcrest.Matcher
import org.hamcrest.Matchers.sameInstance
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The navigation drawer: its items, the four destinations, toolbar title, highlight and Back. */
@RunWith(AndroidJUnit4::class)
class DrawerNavigationTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    // The Video screen's MediaController is a separate window, so every check explicitly looks
    // inside MainActivity's own window.
    private lateinit var mainWindow: Matcher<Root>

    @Before
    fun setUp() {
        activityRule.scenario.onActivity { activity ->
            mainWindow = withDecorView(sameInstance(activity.window.decorView))
        }
    }

    @Test
    fun drawerContainsExactlyTheFourFeaturesInOrder() {
        activityRule.scenario.onActivity { activity ->
            val menu = activity.findViewById<NavigationView>(R.id.nav_view).menu
            val titles = (0 until menu.size()).map { index -> menu.getItem(index).title.toString() }
            assertEquals(listOf("Broadcast Receiver", "Image Scale", "Video", "Audio"), titles)
        }
    }

    @Test
    fun drawerHeaderShowsTheStudentDetails() {
        openDrawer()
        onView(withText("Arafat Haque Biswas")).inRoot(mainWindow).check(matches(isDisplayed()))
        onView(withText("ID: 24241325")).inRoot(mainWindow).check(matches(isDisplayed()))
    }

    @Test
    fun drawerOpensAllFourDestinations() {
        assertOnScreen("Broadcast Receiver", R.id.broadcast_type_spinner)

        navigateTo(R.id.nav_image_scale)
        assertOnScreen("Image Scale", R.id.zoomable_image)

        navigateTo(R.id.nav_audio)
        assertOnScreen("Audio", R.id.play_pause_button)

        navigateTo(R.id.nav_broadcast)
        assertOnScreen("Broadcast Receiver", R.id.broadcast_type_spinner)

        // Last, because its MediaController opens an extra window on top of the Activity.
        navigateTo(R.id.nav_video)
        assertOnScreen("Video", R.id.video_view)
    }

    @Test
    fun backClosesTheOpenDrawerBeforeLeavingTheScreen() {
        navigateTo(R.id.nav_image_scale)
        openDrawer()

        pressBack()

        retryUntilPasses {
            onView(withId(R.id.drawer_layout)).inRoot(mainWindow).check(matches(isClosed(Gravity.START)))
        }
        assertToolbarTitle("Image Scale") // Back only closed the drawer
    }

    private fun openDrawer() {
        onView(withId(R.id.drawer_layout)).inRoot(mainWindow).perform(DrawerActions.open())
    }

    private fun navigateTo(@IdRes menuItemId: Int) {
        openDrawer()
        onView(withId(R.id.nav_view)).inRoot(mainWindow).perform(NavigationViewActions.navigateTo(menuItemId))
    }

    private fun assertOnScreen(title: String, @IdRes viewOnThatScreen: Int) {
        assertToolbarTitle(title)
        // Top-level screens show the hamburger icon, not an Up arrow.
        onView(withContentDescription(androidx.navigation.ui.R.string.nav_app_bar_open_drawer_description))
            .inRoot(mainWindow)
            .check(matches(isDisplayed()))
        onView(withId(viewOnThatScreen)).inRoot(mainWindow).check(matches(isDisplayed()))
        onView(withText(R.string.footer_watermark)).inRoot(mainWindow).check(matches(isDisplayed()))
        activityRule.scenario.onActivity { activity ->
            val highlighted = activity.findViewById<NavigationView>(R.id.nav_view).checkedItem
            assertEquals(title, highlighted?.title.toString())
        }
    }

    private fun assertToolbarTitle(title: String) {
        onView(withId(R.id.toolbar)).inRoot(mainWindow).check(matches(hasDescendant(withText(title))))
    }
}
