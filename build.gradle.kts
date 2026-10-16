plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.spotless)
    alias(libs.plugins.kover)
}

// Root-level Spotless covers every module in the repository in one pass.
spotless {
    kotlin {
        target("*.kt", "**/*.kt")
        targetExclude("**/build/**", "**/node_modules/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
    kotlinGradle {
        target("*.gradle.kts", "**/*.gradle.kts")
        targetExclude("**/build/**", "**/node_modules/**", "**/.gradle/**")
        ktlint(libs.versions.ktlint.get())
    }
}

// Kover 0.9 does not propagate automatically; every module contributes coverage.
subprojects {
    apply(plugin = "org.jetbrains.kotlinx.kover")
}
