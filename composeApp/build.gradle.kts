plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.roborazzi)

    id("kotlin-parcelize")
    id("androidx.baselineprofile")
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    androidTarget()

    compilerOptions {
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        freeCompilerArgs.addAll("-Xexplicit-backing-fields")
    }
}

android {
    namespace = "com.croniot.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    // Map existing KMP directory layout to Android source sets
    sourceSets["main"].java.srcDirs("src/androidMain/kotlin")
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
    sourceSets["test"].java.srcDirs("src/test/kotlin")

    defaultConfig {
        applicationId = "com.croniot.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 14
        versionName = "1.0-alpha10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystorePath = System.getenv("ANDROID_UPLOAD_KEYSTORE_PATH")

    if (keystorePath != null) {
        signingConfigs {
            create("release") {
                storeFile = file(keystorePath)
                storePassword = System.getenv("ANDROID_UPLOAD_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ANDROID_UPLOAD_KEY_ALIAS")
                keyPassword = System.getenv("ANDROID_UPLOAD_KEY_PASSWORD")
                storeType = "PKCS12"
            }
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "/META-INF/{LICENSE.md,LICENSE-notice.md}"
        }
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePath != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }
    ndkVersion = "27.0.11902837 rc2"

    testOptions {
        animationsDisabled = true

        unitTests {
            isIncludeAndroidResources = true
            all {
                it.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }
        }
    }
}

dependencies {

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
    implementation(libs.material.icons.extended)
    implementation(libs.compose.ui)
    implementation(libs.compose.resources)
    implementation(libs.compose.ui.tooling.preview)

    implementation(libs.koin.android)
    implementation(libs.koin.compose)
    implementation(libs.navigationCompose)
    implementation(libs.serialization.json)
    implementation(libs.coreKtx)
    implementation(libs.lifecycleRuntime)
    implementation(libs.lifecycleViewModelCompose)
    implementation(libs.coroutinesCore)
    implementation(libs.coroutinesAndroid)

    implementation(libs.okhttpLoggingInterceptor)
    implementation(libs.accompanistPermissions)

    // Android-specific
    implementation(libs.androidx.activity.compose)
    implementation(libs.datastore.preferences)
    implementation(libs.compose.ui.tooling)
    implementation(libs.compose.material3.android)
    implementation(libs.koin.androidx.compose)

    // Project dependencies
    implementation(projects.shared)
    implementation(projects.client.features.login)
    implementation(projects.client.presentation)
    implementation(projects.client.core)
    implementation(projects.client.data)
    implementation(projects.client.domain)
    implementation(projects.client.features.sensors)
    implementation(projects.client.features.tasktypes)
    implementation(projects.client.features.taskhistory)
    implementation(projects.client.features.blediscovery)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.common)

    // Test
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.mockk)
    testImplementation(libs.coroutines.test)
    
    // Roborazzi
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.composeUiTestJunit4)

    androidTestImplementation(libs.androidx.ui.test.junit4.android)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.coroutines.test)
    "baselineProfile"(project(":baselineprofile"))

    debugImplementation("org.jetbrains.compose.ui:ui-tooling:${libs.versions.compose.plugin.get()}")
}
