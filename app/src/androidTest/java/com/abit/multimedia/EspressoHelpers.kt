package com.abit.multimedia

import android.os.SystemClock
import android.view.View
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
import androidx.test.runner.lifecycle.Stage
import org.hamcrest.Matcher

/**
 * Repeats [assertion] until it passes or [timeoutMs] runs out. Needed for things Espresso can't
 * wait for by itself, such as a broadcast being delivered or the drawer finishing its slide.
 */
fun retryUntilPasses(timeoutMs: Long = 5_000, assertion: () -> Unit) {
    val deadline = SystemClock.uptimeMillis() + timeoutMs
    while (true) {
        try {
            assertion()
            return
        } catch (error: Throwable) {
            if (SystemClock.uptimeMillis() > deadline) throw error
            SystemClock.sleep(100)
        }
    }
}

/** Waits until a view matching [matcher] is on screen. */
fun waitUntilDisplayed(matcher: Matcher<View>, timeoutMs: Long = 5_000) {
    retryUntilPasses(timeoutMs) { onView(matcher).check(matches(isDisplayed())) }
}

/**
 * Finishes the Activities a test opened on top of MainActivity (Activity 2 and Activity 3).
 * ActivityScenarioRule only closes the Activity it launched itself.
 */
fun finishActivitiesAboveMainActivity() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        val monitor = ActivityLifecycleMonitorRegistry.getInstance()
        listOf(Stage.RESUMED, Stage.PAUSED, Stage.STOPPED, Stage.STARTED, Stage.CREATED)
            .flatMap { stage -> monitor.getActivitiesInStage(stage) }
            .filter { activity -> activity !is MainActivity }
            .forEach { activity -> activity.finish() }
    }
}
