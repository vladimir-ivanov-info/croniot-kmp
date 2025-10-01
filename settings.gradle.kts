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
include(":presentation")
include(":client:features:login")
include(":client:features:home")
include(":client:core")
include(":client:domain")
include(":client:data")
include(":client:features:sensors")
include(":client:presentation")
include(":client:features:tasktypes")
