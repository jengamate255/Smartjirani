pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        // Mapbox SDK artifacts are gated behind their downloads repo, which
        // authenticates with the secret token from local.properties.
        val mapboxToken =
            java.util
                .Properties()
                .apply {
                    rootDir
                        .resolve("local.properties")
                        .takeIf { it.exists() }
                        ?.inputStream()
                        ?.use { load(it) }
                }.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")
        maven {
            url = uri("https://api.mapbox.com/downloads/v2/releases/maven")
            credentials {
                username = "mapbox"
                password = mapboxToken
            }
        }
    }
}

rootProject.name = "SmartJirani"

include(":app")

include(":core:common")
include(":core:network")
include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:navigation")
include(":core:location")
include(":core:notifications")
include(":core:audio")
include(":core:video")
include(":core:realtime")
include(":core:security")

include(":feature:auth")
include(":feature:profile")
include(":feature:neighborhood")
include(":feature:feed")
include(":feature:posts")
include(":feature:comments")
include(":feature:voice")
include(":feature:calls")
include(":feature:conference")
include(":feature:notifications")
include(":feature:baraza")
include(":feature:ujamaa")
include(":feature:messaging")
include(":feature:civic")
