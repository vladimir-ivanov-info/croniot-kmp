plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)

    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)

    id("kotlin-parcelize")
    id("croniot.android.library")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

    androidTarget()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.ui)
                implementation(compose.components.resources)
                implementation(compose.components.uiToolingPreview)
                implementation(projects.shared)

                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.navigationCompose)
                implementation(libs.coreKtx)
                implementation(libs.lifecycleRuntime)
                implementation(libs.lifecycleViewModelCompose)
                implementation(libs.coroutinesCore)
                implementation(libs.coroutinesAndroid)
                implementation(libs.retrofit)
                implementation(libs.okhttp)
                implementation(libs.okhttpLoggingInterceptor)
                implementation(libs.mqtt)
                implementation(libs.accompanistPermissions)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(libs.mockk)
                implementation(libs.coroutines.test)
            }
        }
        val androidMain by getting {
            kotlin.srcDirs("src/main/java", "src/main/kotlin")

            dependencies {
                implementation(compose.preview)
                implementation(libs.androidx.activity.compose)

                implementation(libs.androidx.datastore.core.android)

                implementation(libs.datastore.preferences)
                implementation(libs.datastore.core)

                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.material3.android)

                implementation(libs.koin.androidx.compose)

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
        val androidUnitTest by getting {
            kotlin.srcDirs("src/test/java", "src/test/kotlin")
            dependencies {
                implementation(libs.junit.jupiter)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.composeUiTestJunit4)
                implementation(libs.androidx.test.junit)
                implementation(libs.androidx.espresso.core)
            }
        }
    }
}

android {
    namespace = "com.croniot.client.features.login"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    debugImplementation(libs.compose.ui.test.manifest)
}
