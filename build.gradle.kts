buildscript {
    repositories {
        google()
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        classpath("com.karumi:shot:6.1.0")
    }
}

plugins {
    // Declaring plugins with versions here to be shared across all subprojects
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.androidKmpLibrary) apply false
    alias(libs.plugins.androidx.baselineprofile) apply false
    alias(libs.plugins.android.test) apply false

    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
