rootProject.name = "croniot"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                /*includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")*/
            }
        }
        mavenCentral()
        gradlePluginPortal()
        // Add JBoss repository for Infinispan dependencies
       // maven("https://repository.jboss.org/nexus/content/repositories/releases/")
       // maven ("https://repo1.maven.org/maven2/") // Additional source
        maven("https://repo.eclipse.org/content/repositories/jakarta/") // Jakarta Repository

    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
               /* includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")*/
            }
        }
        mavenCentral()
       // maven("https://repository.jboss.org/nexus/content/repositories/releases/")
       // maven ("https://repo1.maven.org/maven2/") // Additional source
        maven("https://repo.eclipse.org/content/repositories/jakarta/") // Jakarta Repository

    }
}

include(":composeApp")
include(":server")
include(":shared")