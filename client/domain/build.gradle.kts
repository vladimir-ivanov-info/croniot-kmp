plugins {
    alias(libs.plugins.kotlinMultiplatform)
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)
    jvm()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(libs.coroutinesCore)
                implementation(libs.koin.core)
                implementation(projects.shared)
            }
        }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}
