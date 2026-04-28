plugins {
    alias(libs.plugins.kotlinJvm)
}

dependencies {
    implementation(libs.coroutinesCore)
    implementation(projects.shared)
    implementation(projects.client.domain)
}
