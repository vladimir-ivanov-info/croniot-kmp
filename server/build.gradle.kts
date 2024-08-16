plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application
}

group = "com.croniot.server"
version = "1.0.0"
application {
    mainClass.set("com.croniot.server.ApplicationKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit)

    implementation(libs.mysql.connector.java)
    implementation(libs.coroutinesCore)
    //implementation(libs.gson)
    implementation(libs.paho.mqtt)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx)
    implementation(libs.ktor.serialization.gson)

    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.cors)
    implementation(libs.logback.classic)
    //implementation(files(libs.commons)) // Use the file path directly
    implementation(libs.postgresql)
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.entitymanager)
    implementation(libs.ehcache)
    implementation(libs.hibernate.ehcache)
    implementation(libs.javax.persistence)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation(libs.serialization.json)
    implementation(libs.dagger)
    //kapt(libs.dagger.compiler)

    testImplementation(libs.mockk)
    testImplementation(libs.h2)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.ktor.server.tests)
    testImplementation(libs.kotlin.test.junit5)



}