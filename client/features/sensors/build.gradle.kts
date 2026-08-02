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
        namespace = "com.croniot.client.features.sensors"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        withHostTestBuilder {}.configure {}
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

                implementation(libs.androidx.datastore.core.android)

                implementation(libs.datastore.preferences)
                implementation(libs.datastore.core)

                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.material3.android)

                implementation(libs.koin.androidx.compose)

                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)

                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.navigationCompose)
                implementation(libs.coreKtx)
                implementation(libs.lifecycleRuntime)
                implementation(libs.lifecycleViewModelCompose)
                implementation(libs.coroutinesCore)
                implementation(libs.coroutinesAndroid)
                implementation(libs.accompanistPermissions)

                // Project dependencies
                implementation(projects.client.core)
                implementation(projects.client.data)
                implementation(projects.client.domain)
                implementation(projects.client.presentation)
                implementation(projects.shared)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.material)
            }
        }
        val androidHostTest by getting {
            kotlin.srcDirs("src/test/java", "src/test/kotlin")

            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)

                implementation(libs.turbine)
                implementation(libs.mockk)
                implementation(libs.coroutines.test)
                implementation(projects.testing.fakes)
            }
        }
    }
}
