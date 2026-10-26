package com.abit.multimedia.broadcast

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BroadcastModeTest {

    @Test
    fun spinnerPositionsMapToModesInTheSpinnerOrder() {
        assertEquals(BroadcastMode.CUSTOM, BroadcastMode.fromSpinnerPosition(0))
        assertEquals(BroadcastMode.BATTERY, BroadcastMode.fromSpinnerPosition(1))
    }

    @Test
    fun invalidSpinnerPositionGivesNull() {
        assertNull(BroadcastMode.fromSpinnerPosition(-1)) // Spinner.INVALID_POSITION
        assertNull(BroadcastMode.fromSpinnerPosition(2))
    }

    @Test
    fun everyModeSurvivesTheTripThroughAnIntentExtra() {
        BroadcastMode.entries.forEach { mode ->
            assertEquals(mode, BroadcastMode.fromExtraValue(mode.name))
        }
    }

    @Test
    fun missingOrUnknownExtraValueGivesNull() {
        assertNull(BroadcastMode.fromExtraValue(null))
        assertNull(BroadcastMode.fromExtraValue(""))
        assertNull(BroadcastMode.fromExtraValue("battery")) // names are case-sensitive
        assertNull(BroadcastMode.fromExtraValue("WIFI"))
    }
}
