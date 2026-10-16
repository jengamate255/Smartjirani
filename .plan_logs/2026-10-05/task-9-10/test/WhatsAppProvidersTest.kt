package com.smartjirani.feature.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WhatsAppProvidersTest {
    @Test
    fun `whatsapp_twilio name correct`() {
        val cfg =
            CivicChannelConfig(
                twilioAccountSid = "AC_test",
                twilioAuthToken = "tok",
                twilioWhatsappFrom = "+15555550100",
                whatsappProvider = "twilio",
            )
        assertEquals("whatsapp_twilio", WhatsAppTwilioProvider(cfg).name)
    }

    @Test
    fun `whatsapp_twilio returns sent on 201`() =
        runTest {
            val cfg =
                CivicChannelConfig(
                    twilioAccountSid = "AC_test",
                    twilioAuthToken = "tok",
                    twilioWhatsappFrom = "+15555550100",
                    whatsappProvider = "twilio",
                )
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(status = 201, body = """{"sid":"SM_wa_123","status":"queued"}""")
                }
            val result =
                WhatsAppTwilioProvider(cfg, http = fake).deliver(
                    CivicNotificationPayload("n1", "u1", "+255712345678", "Test", "Hello"),
                )
            assertEquals(ChannelResult.Sent("SM_wa_123"), result)
        }

    @Test
    fun `whatsapp_twilio returns failed on no config`() =
        runTest {
            val cfg = CivicChannelConfig(whatsappProvider = "twilio")
            val result =
                WhatsAppTwilioProvider(cfg).deliver(
                    CivicNotificationPayload("n1", "u1", "+255712345678", "Test", "Hello"),
                )
            assertTrue(result is ChannelResult.Failed)
            assertEquals("whatsapp_twilio_no_config", (result as ChannelResult.Failed).code)
        }

    @Test
    fun `whatsapp_africas_talking name correct`() {
        val cfg =
            CivicChannelConfig(
                africasTalkingUsername = "u",
                africasTalkingApiKey = "k",
                africasTalkingWhatsappFrom = "SmartJirani",
                whatsappProvider = "africas_talking",
            )
        assertEquals("whatsapp_africas_talking", WhatsAppAfricasTalkingProvider(cfg).name)
    }

    @Test
    fun `whatsapp_africas_talking returns sent on 201`() =
        runTest {
            val cfg =
                CivicChannelConfig(
                    africasTalkingUsername = "u",
                    africasTalkingApiKey = "k",
                    africasTalkingWhatsappFrom = "SmartJirani",
                    whatsappProvider = "africas_talking",
                )
            val fake =
                FakeHttp { _, _ ->
                    HttpResponse(
                        status = 201,
                        body = """{"SMSMessageData":{"MessageId":"AT-WA-1","Recipients":[{"status":"Success"}]}}""",
                    )
                }
            val result =
                WhatsAppAfricasTalkingProvider(cfg, http = fake).deliver(
                    CivicNotificationPayload("n1", "u1", "+255712345678", "Test", "Hello"),
                )
            assertEquals(ChannelResult.Sent("AT-WA-1"), result)
        }

    @Test
    fun `whatsapp_africas_talking returns failed on no config`() =
        runTest {
            val cfg = CivicChannelConfig(whatsappProvider = "africas_talking")
            val result =
                WhatsAppAfricasTalkingProvider(cfg).deliver(
                    CivicNotificationPayload("n1", "u1", "+255712345678", "Test", "Hello"),
                )
            assertTrue(result is ChannelResult.Failed)
            assertEquals("whatsapp_at_no_config", (result as ChannelResult.Failed).code)
        }
}
