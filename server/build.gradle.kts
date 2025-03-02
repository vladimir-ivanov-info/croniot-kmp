import org.gradle.api.tasks.Delete

plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    application

    id("com.github.johnrengelman.shadow") version "8.1.1"

    kotlin("kapt")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.croniot.server"
version = "1.0.0"
application {
    mainClass.set("MainKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    testImplementation(libs.ktor.server.tests)
   /// testImplementation(libs.kotlin.test.junit)
   // implementation(libs.kotlin.stdlib)  // Kotlin standard library

    // JUnit 5 dependencies
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // Kotlin test with JUnit 5
    testImplementation(libs.kotlin.test.junit5)
    implementation(libs.mysql.connector.java)
    implementation(libs.coroutinesCore)
    implementation(libs.paho.mqtt)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx)
    implementation(libs.ktor.serialization.gson)

    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.cors)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    implementation(libs.hibernate.core)
    implementation(libs.hibernate.entitymanager)
    implementation(libs.ehcache)

    implementation(libs.jaxb.impl)

    implementation(libs.jakarta.jaxb.api)
    implementation(libs.jaxb.runtime)
    implementation(libs.jaxb.runtime.old)
    implementation("javax.activation:activation:1.1.1") // For JAXB dependencies
    implementation("javax.xml.bind:jaxb-api:2.3.1")


    implementation(libs.jakarta.activation.api)

    implementation(libs.jcache)
    implementation(libs.hibernate.jcache)
    implementation(libs.hibernate.ehcache)

    // Infinispan dependencies
    implementation(libs.infinispan.hibernate.cache)
    implementation(libs.infinispan.hibernate.cache.commons)
    implementation(libs.infinispan.core)
    implementation(libs.javax.cache.api)

    implementation(libs.javax.persistence)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation(libs.serialization.json)
    implementation(libs.dagger)
    kapt(libs.dagger.compiler)

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

tasks.test {
    useJUnitPlatform()  // Enables JUnit 5 support
}

tasks {
    compileKotlin {
        //kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.jvmTarget = "21"
    }

    compileTestKotlin {
        //kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.jvmTarget = "21"
    }

    shadowJar {
        mergeServiceFiles() //necessary for incñusion of Inifinispan dependencies
        archiveClassifier.set("all") //necessary for incñusion of Inifinispan dependencies

        // Set the main class for the JAR (replace com.example.MainClass with your actual main class)
        manifest {
            attributes["Main-Class"] = "MainKt"
        }

        // Optionally, configure the shadowJar task further if needed
        // For example, excluding files or dependencies, or merging services files

        // Example of excluding certain files from the JAR
        exclude("META-INF/*.DSA", "META-INF/*.SF", "META-INF/*.RSA")
    }
}


// Register a task that deletes directories starting with "KotlinMQTT" in project/server
tasks.register<Delete>("cleanKotlinMQTTFolders") {
    group = "pre-build"
    description = "Removes all folders starting with 'KotlinMQTT' in the croniot/server directory."

    val serverDir = file("../")

    if (serverDir.exists() && serverDir.isDirectory) {
        val dirsToDelete = serverDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("KotlinMQTTServer")
        }?.toList() ?: emptyList()

        delete(dirsToDelete)
    }
}

// Ensure that every other task depends on the cleanKotlinMQTTFolders task,
// so that the deletion happens before any other task runs.
tasks.matching { it.name != "cleanKotlinMQTTFolders" }.configureEach {
    dependsOn("cleanKotlinMQTTFolders")
}