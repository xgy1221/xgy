package com.xgy.androidkaraoke.remote

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanAddressesTest {
    @Test
    fun tipWarnsWhen172SelectedBut192Available() {
        val all = listOf(
            LanAddresses.Candidate("192.168.50.28", "wlan0", "wlan0", 190),
            LanAddresses.Candidate("172.16.1.4", "tun0", "tun0", 10),
        )
        val tip = LanAddresses.tipIfSuspicious("172.16.1.4", all)
        assertNotNull(tip)
        assertTrue(tip!!.contains("192.168"))
    }

    @Test
    fun noTipForHomeWifi() {
        val all = listOf(
            LanAddresses.Candidate("192.168.50.28", "wlan0", "wlan0", 190),
        )
        assertNull(LanAddresses.tipIfSuspicious("192.168.50.28", all))
    }
}
