import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)

            //implementation(libs.google.maps)
            //implementation(libs.maplibreGl)
            implementation("org.maplibre.gl:android-sdk:11.3.0")
           // implementation("org.maplibre.gl:plugin-annotation:11.3.0")
            implementation("org.maplibre.gl:android-plugin-annotation-v9:3.0.0")

            //   implementation("org.maplibre.gl:android-sdk-extensions:11.3.0")
            // implementation("org.maplibre.gl:android-sdk:9.5.0")

        }

        val androidInstrumentedTest by getting {
            dependencies {
                implementation(libs.composeUiTestJunit4)
            }
        }

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(projects.shared)

            implementation(libs.koinAndroid)
            implementation(libs.koinCompose)
            implementation(libs.navigationCompose)
            implementation(libs.coreKtx)
            implementation(project.dependencies.platform(libs.kotlinBom))
            implementation(libs.lifecycleRuntime)
            implementation(libs.lifecycleViewModelCompose)
            implementation(libs.coroutinesCore)
            implementation(libs.coroutinesAndroid)
            implementation(libs.retrofit)
            implementation(libs.converterGson)
            implementation(libs.okhttp)
            implementation(libs.okhttpLoggingInterceptor)

            implementation(libs.mqtt)
            implementation(libs.composeMaterial3)





            // implementation(libs.maplibreSdkExtensions)
            implementation(libs.accompanistPermissions)


        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
        }

    }
}

android {
    namespace = "com.croniot.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "com.croniot.android"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    buildToolsVersion = "35.0.0 rc4"
    ndkVersion = "27.0.11902837 rc2"
    dependencies {
        debugImplementation(compose.uiTooling)
    }
}
dependencies {
    testImplementation(libs.junit.jupiter)
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "com.croniot.android"
            packageVersion = "1.0.0"
        }
    }
}

