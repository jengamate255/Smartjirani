import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.google.services)
}

// Secrets resolve as: gradle property -> environment variable -> local.properties.
// CI copies gradle.properties.example verbatim, so example placeholder values
// (all of which contain "your") are ignored rather than shadowing CI env vars.
val localProps =
    Properties().apply {
        rootProject
            .file("local.properties")
            .takeIf { it.exists() }
            ?.inputStream()
            ?.use { load(it) }
    }

fun secret(name: String): String =
    providers
        .gradleProperty(name)
        .orNull
        ?.takeIf { it.isNotBlank() && !it.contains("your", ignoreCase = true) }
        ?: System.getenv(name)?.takeIf { it.isNotBlank() }
        ?: localProps.getProperty(name).orEmpty()

val supabaseUrl: String = secret("SUPABASE_URL")
val supabaseAnonKey: String = secret("SUPABASE_ANON_KEY")

// PesaPal checkout credentials (sandbox by default; leave blank to run simulated funding).
val pesaPalConsumerKey: String = secret("PESAPAL_CONSUMER_KEY")
val pesaPalConsumerSecret: String = secret("PESAPAL_CONSUMER_SECRET")
val pesaPalEnvironment: String = secret("PESAPAL_ENV").ifBlank { "sandbox" }
val pesaPalCallbackUrl: String = secret("PESAPAL_CALLBACK_URL")

// Release signing credentials (gitignored gradle.properties or CI env)
val smartjiraniKeystorePath: String = secret("SMARTJIRANI_KEYSTORE_PATH")
val smartjiraniKeystorePassword: String = secret("SMARTJIRANI_KEYSTORE_PASSWORD")
val smartjiraniKeyAlias: String = secret("SMARTJIRANI_KEY_ALIAS")
val smartjiraniKeyPassword: String = secret("SMARTJIRANI_KEY_PASSWORD")

android {
    namespace = "com.smartjirani.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.smartjirani.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "PESAPAL_CONSUMER_KEY", "\"$pesaPalConsumerKey\"")
        buildConfigField("String", "PESAPAL_CONSUMER_SECRET", "\"$pesaPalConsumerSecret\"")
        buildConfigField("String", "PESAPAL_ENV", "\"$pesaPalEnvironment\"")
        buildConfigField("String", "PESAPAL_CALLBACK_URL", "\"$pesaPalCallbackUrl\"")
    }

    signingConfigs {
        create("release") {
            if (smartjiraniKeystorePath.isNotBlank()) {
                storeFile = rootProject.file(smartjiraniKeystorePath)
            } else {
                // Use a dummy file when signature not configured
                storeFile = rootProject.file("dummy-release-keystore.jks")
            }
            storePassword =
                if (smartjiraniKeystorePassword.isNotBlank()) {
                    smartjiraniKeystorePassword
                } else {
                    "password123" // placeholder password for dummy keystore
                }
            keyAlias =
                if (smartjiraniKeyAlias.isNotBlank()) {
                    smartjiraniKeyAlias
                } else {
                    "dummy-key-alias"
                }
            keyPassword =
                if (smartjiraniKeyPassword.isNotBlank()) {
                    smartjiraniKeyPassword
                } else {
                    "password123" // placeholder password for dummy key
                }
        }
    }

    buildTypes {
        release {
            if (smartjiraniKeystorePath.isNotBlank() && smartjiraniKeystorePassword.isNotBlank()) {
                // Release keystore available - enable optimization
                isMinifyEnabled = true
                isShrinkResources = true
                proguardFiles(
                    getDefaultProguardFile("proguard-android-optimize.txt"),
                    "proguard-rules.pro",
                )
            } else {
                // No signature config - disable optimization to avoid errors
                isMinifyEnabled = false
                isShrinkResources = false
            }
        }
        debug {
            isMinifyEnabled = false
        }
    }

    splits {
        abi {
            isEnable = true
            reset()
            // x86/x86_64 (emulator-only) dropped to keep direct-APK downloads small for mobile data.
            // x86_64 re-added so the app can run on a local x86_64 emulator.
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        // Console output in CI; full HTML/XML reports are uploaded as artifacts.
        textReport = true
        htmlReport = true
        xmlReport = true
    }
}

dependencies {
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":core:location"))
    implementation(project(":core:notifications"))
    implementation(project(":core:audio"))
    implementation(project(":core:video"))
    implementation(project(":core:realtime"))
    implementation(project(":core:security"))

    implementation(project(":feature:auth"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:neighborhood"))
    implementation(project(":feature:feed"))
    implementation(project(":feature:posts"))
    implementation(project(":feature:comments"))
    implementation(project(":feature:voice"))
    implementation(project(":feature:calls"))
    implementation(project(":feature:conference"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:baraza"))
    implementation(project(":feature:ujamaa"))
    implementation(project(":feature:messaging"))
    implementation(project(":feature:civic"))

    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.coroutines.android)
    implementation(libs.serialization.json)

    implementation(libs.coil.compose)
    implementation(libs.work.runtime.ktx)

    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging")

    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    coreLibraryDesugaring(libs.desugar)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
