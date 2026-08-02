import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kover)
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    androidLibrary {
        namespace = "com.croniot.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    jvm()
    iosArm64()
    iosX64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(libs.serialization.json)
            implementation(libs.mqtt)
            implementation(libs.coroutinesCore)
        }
        val jvmTest by getting {
            dependencies {
                implementation(libs.junit.jupiter)
                runtimeOnly(libs.junit.jupiter.engine)
                runtimeOnly(libs.junit.platform.launcher)
                implementation(libs.assertk)
            }
        }
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}
