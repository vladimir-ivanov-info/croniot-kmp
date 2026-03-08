plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)

    alias(libs.plugins.compose.compiler)
}

kotlin {
    androidLibrary {
        namespace = "com.croniot.client.domain"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    jvmToolchain(21)
    applyDefaultHierarchyTemplate()

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
                implementation(projects.shared)
                implementation(libs.androidx.core.ktx)
                implementation(libs.androidx.appcompat)
                implementation(libs.androidx.material)
            }
        }
    }
}
