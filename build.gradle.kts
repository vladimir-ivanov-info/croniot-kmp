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

    pluginManager.withPlugin("io.gitlab.arturbosch.detekt") {
        extensions.configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
            buildUponDefaultConfig = true
            allRules = false
            autoCorrect = true
            config.setFrom(files("$rootDir/config/detekt/detekt.yml"))
            baseline = file("$rootDir/config/detekt/baseline.xml")
            source.setFrom(
                "src/commonMain/kotlin",
                "src/androidMain/kotlin",
                "src/jvmMain/kotlin",
                "src/main/kotlin",
            )
        }

        tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
            setSource(files(project.projectDir))
            include("**/*.kt", "**/*.kts")
            exclude(
                "**/*.gradle.kts",
                "**/build/**",
                "**/.gradle/**",
                "**/generated/**",
                "**/resources/**",
            )
        }

        tasks.withType<io.gitlab.arturbosch.detekt.DetektCreateBaselineTask>().configureEach {
            setSource(files(project.projectDir))
            include("**/*.kt", "**/*.kts")
            exclude(
                "**/*.gradle.kts",
                "**/build/**",
                "**/.gradle/**",
                "**/generated/**",
                "**/resources/**",
            )
        }

        dependencies {
            "detektPlugins"("dev.androidbroadcast.rules.koin:detekt-koin4-rules:1.0.0")
            "detektPlugins"("io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")
        }
    }
}
