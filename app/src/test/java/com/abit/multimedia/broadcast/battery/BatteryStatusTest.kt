package com.abit.multimedia.broadcast.battery

import android.os.BatteryManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BatteryStatusTest {

    @Test
    fun percentageIsLevelTimes100DividedByScale() {
        assertEquals(42, BatteryStatus.percentageOf(level = 42, scale = 100))
        assertEquals(84, BatteryStatus.percentageOf(level = 21, scale = 25))
        assertEquals(50, BatteryStatus.percentageOf(level = 1, scale = 2))
        assertEquals(100, BatteryStatus.percentageOf(level = 100, scale = 100))
        assertEquals(0, BatteryStatus.percentageOf(level = 0, scale = 100))
    }

    @Test
    fun integerDivisionRoundsDown() {
        assertEquals(33, BatteryStatus.percentageOf(level = 1, scale = 3))
    }

    @Test
    fun scaleOfZeroOrLessGivesNullInsteadOfDividingByZero() {
        assertNull(BatteryStatus.percentageOf(level = 50, scale = 0))
        assertNull(BatteryStatus.percentageOf(level = 50, scale = -1))
    }

    @Test
    fun missingLevelGivesNull() {
        assertNull(BatteryStatus.percentageOf(level = -1, scale = 100))
    }

    @Test
    fun levelAboveScaleIsClampedTo100() {
        assertEquals(100, BatteryStatus.percentageOf(level = 120, scale = 100))
    }

    @Test
    fun chargingStatusMapsBatteryManagerValues() {
        assertEquals(ChargingStatus.CHARGING, ChargingStatus.fromExtraStatus(BatteryManager.BATTERY_STATUS_CHARGING))
        assertEquals(ChargingStatus.DISCHARGING, ChargingStatus.fromExtraStatus(BatteryManager.BATTERY_STATUS_DISCHARGING))
        assertEquals(ChargingStatus.FULL, ChargingStatus.fromExtraStatus(BatteryManager.BATTERY_STATUS_FULL))
        assertEquals(ChargingStatus.NOT_CHARGING, ChargingStatus.fromExtraStatus(BatteryManager.BATTERY_STATUS_NOT_CHARGING))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromExtraStatus(BatteryManager.BATTERY_STATUS_UNKNOWN))
        assertEquals(ChargingStatus.UNKNOWN, ChargingStatus.fromExtraStatus(-1))
    }
}
