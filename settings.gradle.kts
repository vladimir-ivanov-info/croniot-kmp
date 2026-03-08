rootProject.name = "croniot"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
            }
        }
        mavenCentral()
    }
}

include(":composeApp")
include(":server")
include(":shared")
include(":client:features:login")
include(":client:core")
include(":client:domain")
include(":client:data")
include(":client:features:sensors")
include(":client:presentation")
include(":client:features:tasktypes")

include(":baselineprofile")
