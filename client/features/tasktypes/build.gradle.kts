plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)

    alias(libs.plugins.jetbrainsCompose)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(17)
    applyDefaultHierarchyTemplate()

    androidTarget()

    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        val commonMain by getting {
            dependencies {
            }
        }
        val androidMain by getting {
            kotlin.srcDirs("src/main/java", "src/main/kotlin")

            dependencies {
                implementation(libs.androidx.activity.compose)

                implementation(libs.androidx.datastore.core.android)

                implementation("androidx.datastore:datastore-preferences:1.1.2")
                implementation("androidx.datastore:datastore-core:1.1.2")

                implementation(libs.compose.ui)
                implementation(libs.compose.ui.tooling)
                implementation("androidx.compose.material3:material3:1.3.1")

                implementation(libs.koin.androidx.compose)

                implementation("androidx.compose.material:material-icons-extended:1.7.8")

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
                implementation(libs.converterGson)
                implementation(libs.okhttp)
                implementation(libs.okhttpLoggingInterceptor)

                implementation(libs.mqtt)
                implementation(libs.accompanistPermissions)

                // Project dependencies
                implementation(projects.client.core)
                implementation(projects.client.data)
                implementation(projects.shared)
                implementation(projects.client.domain)
                implementation(projects.client.presentation)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.material)
            }
        }
    }
}

android {
    namespace = "com.croniot.client.features.tasktypes"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
