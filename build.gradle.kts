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
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.kover)

    id("org.jlleitschuh.gradle.ktlint") version "13.1.0"
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

dependencies {
    kover(project(":shared"))
    kover(project(":client:core"))
    kover(project(":client:data"))
    kover(project(":client:domain"))
    kover(project(":client:presentation"))
    kover(project(":client:features:login"))
    kover(project(":client:features:sensors"))
    kover(project(":client:features:blediscovery"))
    kover(project(":client:features:taskhistory"))
    kover(project(":client:features:tasktypes"))
    kover(project(":composeApp"))
}

// Coverage target is "100% of what a unit test can meaningfully protect". The exclusions below are
// code that unit tests cannot verify in a way that catches real regressions:
//   - Compose UI (Screens, components, theme): needs snapshot/instrumentation testing, a different
//     discipline than unit tests. Composing without throwing proves nothing about correctness.
//   - Koin DI modules (single{}/factory{} wiring): a test here only proves Kotlin calls what you
//     tell it to call.
//   - Android lifecycle classes (Activity/Application/Service): need full instrumentation;
//     MyApp specifically starts Koin as a real side effect that collides with Robolectric-based
//     unit tests in the same JVM (see docs/test-coverage.md, hallazgo 13).
//   - Hardware/Keystore-coupled classes (real BLE GATT connection flow, real MQTT client,
//     EncryptedSharedPreferences): require a physical device or broker, not mockable meaningfully.
//   - Pure interfaces and constant-only objects: no executable logic to protect.
kover {
    reports {
        filters {
            excludes {
                packages(
                    "*.di",
                    "*.generated.resources",
                    "com.croniot.android.core.presentation.theme",
                    "com.croniot.client.presentation.components",
                    "com.croniot.client.presentation.constants",
                    "com.croniot.client.core.config",
                    "com.croniot.client.domain.repositories",
                    "com.croniot.android.core.services",
                )
                classes(
                    // Compose screens / screen-level composables (one per feature)
                    "*ScreenKt*", "*Screen", "*ScreenBody*", "*ItemCard*", "*Item*Kt*",
                    // Outlier: file is named "Screen*" (prefix) instead of "*Screen" (suffix),
                    // so it doesn't match the ScreenKt/Screen suffix patterns above.
                    "com.croniot.android.features.registeraccount.presentation.ScreenCreateAccountKt*",
                    // Compose-compiler-generated synthetic class holding composable lambdas for a
                    // file; one per Kt file with composables, nested under every package — no logic.
                    "*ComposableSingletons*",
                    "com.croniot.android.app.MainActivity",
                    "com.croniot.android.BuildConfig",
                    "com.croniot.android.app.MyApp*",
                    "com.croniot.android.app.CurrentScreenKt*",
                    "com.croniot.android.app.ThemeTestsKt",
                    "com.croniot.android.app.AppRoute*",
                    "com.croniot.client.presentation.CroniotSliderKt*",
                    "com.croniot.client.presentation.PerformanceChartKt*",
                    "com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameterKt*",
                    "com.croniot.client.features.tasktypes.presentation.create_task.parameter.StatefulParameterSliderKt*",
                    // Hardware/Keystore/broker-coupled — cannot be exercised without real devices.
                    // Trailing "*" alone does NOT reliably exclude nested continuation/lambda classes
                    // (e.g. "Impl$connect$1") when the pattern is a fully-qualified prefix — verified
                    // empirically in session 7 that a leading "*" (package-agnostic) is what actually
                    // works, matching the same style already used for "*ComposableSingletons*" below.
                    "*BleConnectionImpl*",
                    "*EncryptedBleCredentialStore*",
                    "*EncryptedTokenStore*",
                    "*RemoteSensorDataSourceImpl*",
                    "MqttHandler*",
                    "com.croniot.android.core.notifications.NotificationHelper",
                    // Room-generated migration bookkeeping (onValidateSchema/dropAllTables/onPreMigrate):
                    // pure codegen with no custom logic, only exercised via a real Migration path which
                    // requires exportSchema=true + MigrationTestHelper instrumentation infra. The actual
                    // queries/entities this wraps are already covered by the DAO test suite (~100%).
                    "*AppDatabase_Impl*",
                )
            }
        }
    }
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
