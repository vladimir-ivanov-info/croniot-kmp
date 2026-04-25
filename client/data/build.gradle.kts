plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    id("croniot.android.library")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)
    applyDefaultHierarchyTemplate()
    androidTarget()
    iosArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(kotlin("stdlib"))
                implementation(libs.room.runtime)
                implementation(libs.compose.runtime)
                implementation(libs.coroutinesCore)
                implementation(libs.koin.core)
                implementation(libs.serialization.json)
                implementation(projects.shared)
            }
        }

        val androidUnitTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
                implementation(libs.mockk)
                implementation(libs.coroutines.test)
                implementation(libs.ktor.client.mock)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx)
            }
        }

        val androidMain by getting {
            kotlin.srcDirs("src/main/java", "src/main/kotlin")
            dependencies {
                implementation(projects.client.core)
                implementation(projects.client.domain)
                implementation(libs.androidx.activity.compose)
                implementation(libs.androidx.datastore.core.android)
                implementation(libs.datastore.preferences)
                implementation(libs.datastore.core)
                implementation(libs.androidx.security.crypto)
                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling)
                implementation(libs.compose.material3.android)
                implementation(libs.koin.androidx.compose)
                implementation(libs.koin.android)
                implementation(libs.koin.compose)
                implementation(libs.navigationCompose)
                implementation(libs.coreKtx)
                implementation(libs.lifecycleRuntime)
                implementation(libs.lifecycleViewModelCompose)
                implementation(libs.coroutinesAndroid)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.okhttp)
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.client.auth)
                implementation(libs.ktor.client.logging)
                implementation(libs.ktor.serialization.kotlinx)
                implementation(libs.mqtt)
                implementation(libs.accompanistPermissions)
            }
        }
    }
}

android {
    namespace = "com.croniot.client.data"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

dependencies {
    add("kspAndroid", libs.room.compiler)
    add("kspIosArm64", libs.room.compiler)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.material)
}
