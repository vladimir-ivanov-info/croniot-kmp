plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.jetbrainsCompose) apply false
    alias(libs.plugins.compose.compiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false

    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
}

// Necesario porque el plugin de Shot no viene del Plugin Portal con id,
// sino como classpath de buildscript
buildscript {
    repositories {
        google()
        mavenCentral()
        // (opcional) gradle plugin portal por si hace falta
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
        // ✅ Kotlin DSL: usa paréntesis
        classpath("com.karumi:shot:6.1.0")
    }
}

subprojects {
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}