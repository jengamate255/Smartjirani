package com.smartjirani.feature.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SmsAfricasTalkingProviderTest {
    private fun cfg() =
        CivicChannelConfig(
            africasTalkingUsername = "u",
            africasTalkingApiKey = "k",
            africasTalkingFrom = "SmartJirani",
            smsProvider = "africas_talking",
        )

    @Test
    fun `name is sms_africas_talking`() {
        assertEquals("sms_africas_talking", SmsAfricasTalkingProvider(cfg()).name)
    }

    @Test
    fun `deliver returns sent on 201`() =
        runTest {
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(
                        status = 201,
                        body = """{"SMSMessageData":{"MessageId":"AT-1","Recipients":[{"status":"Success"}]}}""",
                    )
                }
            val provider = SmsAfricasTalkingProvider(cfg(), http = fake)
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
            assertEquals(ChannelResult.Sent("AT-1"), result)
        }

    @Test
    fun `deliver returns failed on 4xx`() =
        runTest {
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(status = 401, body = """{"error":"unauthorized"}""")
                }
            val provider = SmsAfricasTalkingProvider(cfg(), http = fake)
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
