plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)

    alias(libs.plugins.compose.compiler)
    id("io.gitlab.arturbosch.detekt")
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
                implementation(libs.androidx.datastore.core.android)
                implementation(libs.koin.android)
                implementation(libs.coreKtx)
                implementation(libs.coroutinesCore)

                // Project dependencies
                implementation(projects.client.core)
                implementation(projects.shared)
            }
        }
    }
}
