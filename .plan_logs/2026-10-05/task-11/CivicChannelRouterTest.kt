package com.smartjirani.feature.civic.channels

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CivicChannelRouterTest {
    private fun baseCfg() =
        CivicChannelConfig(
            twilioAccountSid = "AC",
            twilioAuthToken = "tok",
            twilioFromNumber = "+15555550100",
            smsProvider = "twilio",
            whatsappProvider = "noop",
        )

    @Test
    fun `planFor sms only routes to sms provider`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("sms")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("sms", "sms_twilio")), plan)
    }

    @Test
    fun `planFor sms,whatsapp routes to sms provider and whatsapp_noop`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("sms,whatsapp")
        assertEquals(
            listOf(
                CivicChannelRouter.PlannedDelivery("sms", "sms_twilio"),
                CivicChannelRouter.PlannedDelivery("whatsapp", "whatsapp_noop"),
            ),
            plan,
        )
    }

    @Test
    fun `planFor with no provider skips`() {
        val cfg = baseCfg().copy(twilioAccountSid = null, twilioAuthToken = null)
        val router = CivicChannelRouter(CivicChannelRegistry(cfg))
        val plan = router.planFor("sms")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("sms", "skipped_no_provider")), plan)
    }

    @Test
    fun `planFor in_app only`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("in_app")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("in_app", "in_app")), plan)
    }

    @Test
    fun `planFor empty pref defaults to in_app`() {
        val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
        val plan = router.planFor("")
        assertEquals(listOf(CivicChannelRouter.PlannedDelivery("in_app", "in_app")), plan)
    }

    @Test
    fun `route returns Skipped for skipped_no_provider`() =
        runTest {
            val cfg = baseCfg().copy(twilioAccountSid = null, twilioAuthToken = null)
            val router = CivicChannelRouter(CivicChannelRegistry(cfg))
            val payload = CivicNotificationPayload("n1", "u1", "+255712345678", "T", "B")
            val planned = router.planFor("sms")
            val results = router.route(payload, planned)
            assertEquals(1, results.size)
            val (planned_delivery, result) = results[0]
            assertEquals("sms", planned_delivery.channel)
            assertTrue(result is ChannelResult.Skipped)
        }

    @Test
    fun `route delivers via WhatsAppNoOp returns Skipped`() =
        runTest {
            val router = CivicChannelRouter(CivicChannelRegistry(baseCfg()))
            val payload = CivicNotificationPayload("n1", "u1", "+255712345678", "T", "B")
            val planned = router.planFor("whatsapp")
            val results = router.route(payload, planned)
            assertEquals(1, results.size)
            val (_, result) = results[0]
            assertTrue(result is ChannelResult.Skipped)
            assertEquals("whatsapp_noop", (result as ChannelResult.Skipped).reason)
        }

    @Test
    fun `fromEnv reads map correctly`() {
        val cfg =
            CivicChannelConfig.fromEnv(
                mapOf(
                    "TWILIO_ACCOUNT_SID" to "AC_test",
                    "TWILIO_AUTH_TOKEN" to "tok",
                    "CIVIC_SMS_PROVIDER" to "twilio",
                ),
            )
        assertEquals("AC_test", cfg.twilioAccountSid)
        assertEquals("tok", cfg.twilioAuthToken)
        assertEquals("twilio", cfg.smsProvider)
        assertEquals("noop", cfg.whatsappProvider) // default
    }

    @Test
    fun `fromEnv defaults`() {
        val cfg = CivicChannelConfig.fromEnv(emptyMap())
        assertEquals("twilio", cfg.smsProvider) // default
        assertEquals("noop", cfg.whatsappProvider) // default
        assertEquals(null, cfg.twilioAccountSid)
    }
}
