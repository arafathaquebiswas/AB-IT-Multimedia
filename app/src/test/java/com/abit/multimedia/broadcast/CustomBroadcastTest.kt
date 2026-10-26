package com.abit.multimedia.broadcast

import org.junit.Assert.assertEquals
import org.junit.Test

class CustomBroadcastTest {

    @Test
    fun actionAndExtraAreBuiltFromTheApplicationId() {
        assertEquals(
            "com.abit.multimedia.action.CUSTOM_MESSAGE",
            CustomBroadcast.action("com.abit.multimedia"),
        )
        assertEquals(
            "com.abit.multimedia.extra.MESSAGE",
            CustomBroadcast.messageExtra("com.abit.multimedia"),
        )
    }

    @Test
    fun namesFollowADifferentApplicationId() {
        assertEquals("com.example.test.action.CUSTOM_MESSAGE", CustomBroadcast.action("com.example.test"))
        assertEquals("com.example.test.extra.MESSAGE", CustomBroadcast.messageExtra("com.example.test"))
    }
}
