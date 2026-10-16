package com.smartjirani.feature.civic.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ProfileChannelPrefEditorTest {
    @Test
    fun `channel options contain all four values`() {
        val options = ChannelOption.ALL
        assertEquals(4, options.size)
        assertEquals("in_app", options[0].value)
        assertEquals("sms", options[1].value)
        assertEquals("whatsapp", options[2].value)
        assertEquals("sms,whatsapp", options[3].value)
    }

    @Test
    fun `current value is resolved to first option when unknown`() {
        val resolved = ChannelOption.resolve("unknown_value")
        assertEquals("in_app", resolved.value)
    }

    @Test
    fun `current value is preserved when known`() {
        val resolved = ChannelOption.resolve("sms")
        assertEquals("sms", resolved.value)
    }

    @Test
    fun `sms_comma_whatsapp is a valid option`() {
        val options = ChannelOption.ALL
        assertTrue(options.any { it.value == "sms,whatsapp" })
    }
}
