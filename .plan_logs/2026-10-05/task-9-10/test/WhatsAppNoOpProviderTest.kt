package com.smartjirani.feature.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhatsAppNoOpProviderTest {
    @Test
    fun `name is whatsapp_noop`() {
        assertEquals("whatsapp_noop", WhatsAppNoOpProvider().name)
    }

    @Test
    fun `deliver always returns Skipped`() =
        runTest {
            val provider = WhatsAppNoOpProvider()
            val result =
                provider.deliver(
                    CivicNotificationPayload(
                        notificationId = "n1",
                        recipientUserId = "u1",
                        toPhone = "+255712345678",
                        title = "Test",
                        body = "Hello",
                    ),
                )
            assertTrue(result is ChannelResult.Skipped)
            assertEquals("whatsapp_noop", (result as ChannelResult.Skipped).reason)
        }
}
