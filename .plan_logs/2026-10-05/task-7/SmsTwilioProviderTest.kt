package com.smartjirani.feature.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsTwilioProviderTest {
    private fun cfg() =
        CivicChannelConfig(
            twilioAccountSid = "AC_test",
            twilioAuthToken = "tok",
            twilioFromNumber = "+15555550100",
            africasTalkingUsername = null,
            africasTalkingApiKey = null,
            africasTalkingFrom = null,
            smsProvider = "twilio",
        )

    @Test
    fun `name is sms_twilio`() {
        assertEquals("sms_twilio", SmsTwilioProvider(cfg()).name)
    }

    @Test
    fun `deliver returns sent on 201`() =
        runTest {
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(status = 201, body = """{"sid":"SM123","status":"queued"}""")
                }
            val provider = SmsTwilioProvider(cfg(), http = fake)
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
            assertEquals("SM123", (result as ChannelResult.Sent).externalId)
        }

    @Test
    fun `deliver returns failed on 4xx`() =
        runTest {
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(status = 403, body = """{"code":20003,"message":"auth"}""")
                }
            val provider = SmsTwilioProvider(cfg(), http = fake)
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
            assertTrue(result is ChannelResult.Failed)
        }
}
