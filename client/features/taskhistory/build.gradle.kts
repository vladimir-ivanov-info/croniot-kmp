plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)

    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kover)
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    androidLibrary {
        namespace = "com.croniot.client.features.taskhistory"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {
            isIncludeAndroidResources = false
        }
    }

    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            kotlin.srcDirs("src/main/java", "src/main/kotlin")

            dependencies {
                implementation(libs.androidx.activity.compose)

                implementation(libs.compose.ui)
                implementation(libs.compose.material3.android)

                implementation(libs.koin.androidx.compose)

                implementation(libs.material.icons.extended)

                implementation(libs.paging.runtime)
                implementation(libs.paging.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.ui)

                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.lifecycleRuntime)
                implementation(libs.lifecycleViewModelCompose)
                implementation(libs.coroutinesCore)
                implementation(libs.coroutinesAndroid)

                // Project dependencies
                implementation(projects.client.core)
                implementation(projects.client.data)
                implementation(projects.client.domain)
                implementation(projects.client.presentation)
                implementation(projects.shared)
            }
        }
        val androidHostTest by getting {
            kotlin.srcDirs("src/test/java", "src/test/kotlin")

            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)

                implementation(libs.turbine)
                implementation(libs.assertk)
                implementation(libs.mockk)
                implementation(libs.coroutines.test)
                implementation(libs.paging.testing)

                implementation(projects.client.domain)
            }
        }
    }
}
