plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    id("croniot.android.library")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)

    applyDefaultHierarchyTemplate() // 🔑 Requerido en Kotlin ≥1.9.20

    androidTarget()

    sourceSets {
        val androidMain by getting {

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
                implementation(libs.retrofit)
                implementation(libs.okhttp)
                implementation(libs.okhttpLoggingInterceptor)

                implementation(libs.mqtt)

                implementation(libs.accompanistPermissions)

                implementation(libs.material.icons.extended)
            }

            val androidInstrumentedTest by getting {
                dependencies {
                    // Compose test rule (alineado con tu stack)
                    implementation("androidx.compose.ui:ui-test-junit4:1.9.1")

                    // Shot (único artefacto necesario para screenshots)
                    implementation(libs.shot.android)

                    // (No declares runner/ext aquí: vendrán transitivamente y evitas conflictos)
                }
            }
        }
    }
}

apply(plugin = "shot")
android {
    namespace = "com.croniot.client.presentation"

    defaultConfig {
        testApplicationId = "com.croniot.client.presentation.test"
        testInstrumentationRunner = "com.karumi.shot.ShotTestRunner"
    }
}

dependencies {
    implementation(projects.client.core)

    implementation(projects.client.domain)
    implementation(projects.shared)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
    implementation(libs.androidx.ui.test.junit4.android)
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    androidTestImplementation(libs.androidx.test.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    debugImplementation(compose.uiTooling)
    debugImplementation(libs.compose.ui.test.manifest)
}