package com.abit.multimedia

import androidx.test.espresso.Espresso.onData
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.closeSoftKeyboard
import androidx.test.espresso.action.ViewActions.typeText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withSpinnerText
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.endsWith
import org.hamcrest.Matchers.instanceOf
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.not
import org.hamcrest.Matchers.startsWith
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Broadcast Receiver flow: Spinner -> Activity 2 -> Activity 3, for both options. */
@RunWith(AndroidJUnit4::class)
class BroadcastFlowTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @After
    fun tearDown() {
        finishActivitiesAboveMainActivity()
    }

    @Test
    fun customMessageIsDeliveredToTheReceiverThroughABroadcast() {
        selectBroadcastType("Custom broadcast receiver")
        onView(withId(R.id.proceed_button)).perform(click())

        // Activity 2
        onView(withId(R.id.message_edit_text)).perform(typeText("Hello CSE489"), closeSoftKeyboard())
        onView(withId(R.id.send_button)).perform(click())

        // Activity 3: the broadcast is delivered asynchronously, so wait for it.
        waitUntilDisplayed(withText("Received via broadcast: Hello CSE489"))
        onView(withId(R.id.receiver_status_text)).check(matches(withText(R.string.receiver_registered)))
        onView(withId(R.id.received_time_text)).check(matches(withText(endsWith("broadcast #1"))))
        onView(withText(R.string.footer_watermark)).check(matches(isDisplayed()))
    }

    @Test
    fun blankMessageShowsAnErrorAndStaysOnActivity2() {
        selectBroadcastType("Custom broadcast receiver")
        onView(withId(R.id.proceed_button)).perform(click())

        onView(withId(R.id.send_button)).perform(click())

        onView(withText(R.string.error_message_blank)).check(matches(isDisplayed()))
        onView(withId(R.id.message_edit_text)).check(matches(isDisplayed()))
    }

    @Test
    fun batteryOptionShowsLiveStatusAndActivity3DoesNothing() {
        selectBroadcastType("System battery notification receiver")
        onView(withId(R.id.proceed_button)).perform(click())

        // Activity 2: filled in from the sticky ACTION_BATTERY_CHANGED broadcast.
        waitUntilDisplayed(allOf(withId(R.id.battery_percent_text), withText(endsWith("%"))))
        onView(withId(R.id.charging_status_text)).check(matches(withText(startsWith("Status: "))))
        onView(withId(R.id.battery_updated_text)).check(matches(withText(startsWith("Last update: "))))
        onView(withText(R.string.footer_watermark)).check(matches(isDisplayed()))

        onView(withId(R.id.next_button)).perform(click())

        // Activity 3 in battery mode: only the note, no custom receiver.
        onView(withId(R.id.battery_note_card)).check(matches(isDisplayed()))
        onView(withId(R.id.custom_receiver_group)).check(matches(not(isDisplayed())))
    }

    private fun selectBroadcastType(option: String) {
        onView(withId(R.id.broadcast_type_spinner)).perform(click())
        onData(allOf(instanceOf(String::class.java), `is`(option))).perform(click())
        onView(withId(R.id.broadcast_type_spinner)).check(matches(withSpinnerText(option)))
    }
}
