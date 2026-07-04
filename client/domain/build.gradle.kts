plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlin.serialization)
    id("io.gitlab.arturbosch.detekt")
}

dependencies {
    implementation(libs.coroutinesCore)
    implementation(libs.koin.core)
    implementation(libs.serialization.json)
    implementation(projects.shared)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.assertk)
    testImplementation(libs.coroutines.test)
    testImplementation(projects.testing.fakes)
}
