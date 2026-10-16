package com.smartjirani.feature.civic.channels

class CivicChannelRegistry(
    private val cfg: CivicChannelConfig,
) {
    fun active(): List<CivicChannelProvider> {
        val providers = mutableListOf<CivicChannelProvider>()
        when (cfg.smsProvider) {
            "twilio" -> {
                if (cfg.twilioAccountSid != null && cfg.twilioAuthToken != null) {
                    providers += SmsTwilioProvider(cfg)
                }
            }

            "africas_talking" -> {
                if (cfg.africasTalkingApiKey != null) {
                    providers += SmsAfricasTalkingProvider(cfg)
                }
            }
        }
        when (cfg.whatsappProvider) {
            "twilio" -> {
                if (cfg.twilioAccountSid != null && cfg.twilioAuthToken != null) {
                    providers += WhatsAppTwilioProvider(cfg)
                }
            }

            "africas_talking" -> {
                if (cfg.africasTalkingApiKey != null) {
                    providers += WhatsAppAfricasTalkingProvider(cfg)
                }
            }

            "noop", null -> {
                providers += WhatsAppNoOpProvider()
            }
        }
        return providers
    }
}
