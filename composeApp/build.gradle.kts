import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)

    id("kotlin-parcelize")
    id("androidx.baselineprofile")
}

apply(plugin = "shot")

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
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

    defaultConfig {
        applicationId = "com.croniot.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 4
        versionName = "1.0"

        testInstrumentationRunner = "com.karumi.shot.ShotTestRunner"
    }

    flavorDimensions += "backend"
    productFlavors {
        create("demo") {
            dimension = "backend"
            applicationIdSuffix = ".demo"
            versionNameSuffix = "-demo"
            buildConfigField("boolean", "IS_DEMO", "true")
            resValue("string", "app_name", "Croniot Demo")
        }
        create("real") {
            dimension = "backend"
            buildConfigField("boolean", "IS_DEMO", "false")
            resValue("string", "app_name", "Croniot")
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("debug") {
            isDebuggable = true
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
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
    }
}

dependencies {

    implementation(libs.compose.runtime)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material)
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
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    implementation(libs.mqtt)
    implementation(libs.accompanistPermissions)

    // Android-specific
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.datastore.core.android)
    implementation("androidx.datastore:datastore-preferences:1.1.2")
    implementation("androidx.datastore:datastore-core:1.1.2")
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling)
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation(libs.koin.androidx.compose)
    implementation("androidx.profileinstaller:profileinstaller:1.4.1")

    // Project dependencies
    implementation(projects.shared)
    implementation(projects.client.features.login)
    implementation(projects.client.presentation)
    implementation(projects.client.core)
    implementation(projects.client.data)
    implementation(projects.client.domain)
    implementation(projects.client.features.sensors)
    implementation(projects.client.features.tasktypes)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.common)

    // Test
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.9.1")
    androidTestImplementation("com.karumi:shot-android:6.1.0")
    "baselineProfile"(project(":baselineprofile"))

    debugImplementation("org.jetbrains.compose.ui:ui-tooling:${libs.versions.compose.plugin.get()}")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.9.1")
}
