import org.gradle.api.tasks.Delete
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.util.Properties
import kotlin.system.exitProcess
import nu.studer.gradle.jooq.JooqEdition
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Property
import org.jooq.meta.jaxb.Target as JooqTarget

val localPropertiesText = providers.fileContents(
    rootProject.layout.projectDirectory.file("local.properties")
).asText

val localProperties = Properties().apply {
    localPropertiesText.orNull?.let { text -> load(StringReader(text)) }
}

plugins {
    alias(libs.plugins.kotlinJvm)
    application
    id("com.gradleup.shadow") version "9.3.2"
    alias(libs.plugins.ksp)
    id("info.solidsoft.pitest") version "1.19.0-rc.3"
    id("nu.studer.jooq") version "10.2"
    alias(libs.plugins.kotlin.serialization)
    id("io.gitlab.arturbosch.detekt")
}

kotlin {
    jvmToolchain(21)
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

group = "com.croniot.server"
version = "1.0.0"
application {
    mainClass.set("com.server.croniot.application.MainKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {
    implementation(projects.shared)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.status.pages)
    implementation(libs.ktor.server.auth)
    implementation(libs.ktor.server.auth.jwt)
    implementation(libs.ktor.server.rate.limit)
    implementation(libs.ktor.server.metrics.micrometer)
    implementation(libs.micrometer.registry.prometheus)
    implementation(libs.jbcrypt)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.kotlin.test.junit5)

    implementation(libs.coroutinesCore)
    implementation(libs.paho.mqtt)
    implementation(libs.ktor.serialization.kotlinx)

    // implementation(libs.ktor.serialization.gson) // REMOVED

    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.cors)
    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging)
    implementation(libs.postgresql)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(libs.mockk)
    testImplementation(libs.h2)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.ktor.server.test.host)

    pitest("org.pitest:pitest-junit5-plugin:1.2.0")

    implementation(libs.jooq.core)
    implementation(libs.hikari)
    jooqGenerator(libs.jooq.meta.extensions)
}

tasks.test {
    useJUnitPlatform()
}

tasks {
    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("all")
        manifest {
            attributes["Main-Class"] = "com.server.croniot.application.MainKt"
        }
        exclude("META-INF/*.DSA", "META-INF/*.SF", "META-INF/*.RSA")
    }
}

tasks.register<Delete>("cleanKotlinMQTTFolders") {
    group = "pre-build"
    val serverDir = file("../")
    if (serverDir.exists() && serverDir.isDirectory) {
        val dirsToDelete = serverDir.listFiles { file ->
            file.isDirectory && file.name.startsWith("KotlinMQTTServer")
        }?.toList() ?: emptyList()
        delete(dirsToDelete)
    }
}

tasks.named("shadowJar") { dependsOn("cleanKotlinMQTTFolders") }

tasks.named("run") {
    dependsOn("cleanKotlinMQTTServers")
}

tasks.register<Delete>("cleanKotlinMQTTServers") {
    val baseDir = rootProject.projectDir
    delete(
        provider {
            baseDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("KotlinMQTTServer") }
                ?: emptyList()
        }
    )
}

jooq {
    edition.set(JooqEdition.OSS)
    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN
                generator = Generator().apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database = Database().apply {
                        name = "org.jooq.meta.extensions.ddl.DDLDatabase"
                        properties = listOf(
                            Property().withKey("scripts").withValue("src/main/resources/schema.sql"),
                            Property().withKey("sort").withValue("semantic"),
                            Property().withKey("unqualifiedSchema").withValue("none"),
                            Property().withKey("defaultNameCase").withValue("lower"),
                            Property().withKey("parseDialect").withValue("POSTGRES"),
                        )
                        inputSchema = "PUBLIC"
                        includes = ".*"
                        excludes = "flyway_schema_history"
                    }
                    generate = Generate().apply {
                        isPojos = true
                        isImmutablePojos = true
                    }
                    target = JooqTarget().apply {
                        packageName = "com.server.croniot.jooq"
                        directory = "${project.layout.buildDirectory.get().asFile}/generated-src/jooq/main"
                    }
                }
            }
        }
    }
}

pitest {
    pitestVersion.set("1.19.0")
    testPlugin.set("junit5")
    targetClasses.set(listOf("com.server.croniot.services.*"))
    targetTests.set(listOf("domain.*"))
    outputFormats.set(listOf("HTML"))
    timestampedReports.set(false)
    threads.set(4)
    mutators.set(listOf("DEFAULTS"))
}
