package com.smartjirani.feature.civic

data class CivicAiConfig(
    val aiProvider: String = "gemini",
    val geminiKey: String = "",
    val geminiModel: String = "gemini-2.0-flash",
)
