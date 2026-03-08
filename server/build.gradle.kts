import org.gradle.api.tasks.Delete
//import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream
import java.util.Properties
import kotlin.system.exitProcess
import nu.studer.gradle.jooq.JooqEdition
import org.gradle.declarative.dsl.schema.FqName.Empty.packageName
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jooq.meta.jaxb.*
import org.jooq.meta.jaxb.Jdbc
import org.jooq.meta.jaxb.Generator
import org.jooq.meta.jaxb.Database
import org.jooq.meta.jaxb.Generate
import org.jooq.meta.jaxb.Logging
import org.jooq.meta.jaxb.Target as JooqTarget

val localProperties = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

//apply(from = "gradle/docker.gradle.kts")


plugins {
    alias(libs.plugins.kotlinJvm)
    application

    id("com.gradleup.shadow") version "9.3.2"

    alias(libs.plugins.ksp)


    id("info.solidsoft.pitest") version "1.19.0-rc.3"

    id("nu.studer.jooq") version "10.2"

}

kotlin{
   // jvmToolchain(21)
    jvmToolchain(21)

    // Sustituye kotlinOptions por el nuevo DSL
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
        // freeCompilerArgs.addAll("-Xjsr305=strict")
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
    //mainClass.set("MainKt")
    mainClass.set("com.server.croniot.application.MainKt")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=${extra["io.ktor.development"] ?: "false"}")
}

dependencies {

    //annotationProcessor(libs.dagger.compiler)

    implementation(projects.shared)
    implementation(libs.logback)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
        //implementation(project(":client:data"))
  //  testImplementation(libs.ktor.server.test.host)
   /// testImplementation(libs.kotlin.test.junit)
   // implementation(libs.kotlin.stdlib)  // Kotlin standard library

    // JUnit 5 dependencies
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)

    // Kotlin test with JUnit 5
    testImplementation(libs.kotlin.test.junit5)
    implementation(libs.coroutinesCore)
    implementation(libs.paho.mqtt)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.serialization.kotlinx)

    implementation(libs.ktor.serialization.gson)
    //implementation(libs.gson)

    implementation(libs.ktor.content.negotiation)
    implementation(libs.ktor.cors)
    implementation(libs.logback.classic)
    implementation(libs.postgresql)
    implementation(libs.mapstruct)
    annotationProcessor(libs.mapstruct.processor)
    implementation(libs.serialization.json)
    implementation(libs.dagger)
    ksp(libs.dagger.compiler)

    testImplementation(libs.mockk)
    testImplementation(libs.h2)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.junit)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlin.test.junit5)


    pitest("org.pitest:pitest-junit5-plugin:1.2.0")

    implementation(libs.jooq.core)
    implementation(libs.hikari)
    jooqGenerator(libs.postgresql)

   /// implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("com.zaxxer:HikariCP:7.0.2")


}

tasks.test {
    useJUnitPlatform()  // Enables JUnit 5 support
}

tasks {
  /*  compileKotlin {
        //kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.jvmTarget = "21"
    }

    compileTestKotlin {
        //kotlinOptions.jvmTarget = "1.8"
        kotlinOptions.jvmTarget = "21"
    }
*/
//    shadowJar {
//        mergeServiceFiles() //necessary for incñusion of Inifinispan dependencies
//        archiveClassifier.set("all") //necessary for incñusion of Inifinispan dependencies
//
//        // Set the main class for the JAR (replace com.example.MainClass with your actual main class)
//        manifest {
//            attributes["Main-Class"] = "MainKt"
//        }
//
//        // Optionally, configure the shadowJar task further if needed
//        // For example, excluding files or dependencies, or merging services files
//
//        // Example of excluding certain files from the JAR
//        exclude("META-INF/*.DSA", "META-INF/*.SF", "META-INF/*.RSA")
//    }
    shadowJar {
        mergeServiceFiles()
        archiveClassifier.set("all")
        manifest {
            attributes["Main-Class"] = "com.server.croniot.application.MainKt"
        }
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



tasks.named("run") {
    //TODO dependsOn("ensureDockerComposeAndWait")

    dependsOn("cleanKotlinMQTTServers")

}

//tasks.register("runDetached") {
//    group = "application"
//    description = "Starts the server as a detached background process, freeing the Gradle daemon immediately."
//    dependsOn("shadowJar", "cleanKotlinMQTTServers")
//    doLast {
//        val jar = tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar")
//            .get().archiveFile.get().asFile
//        ProcessBuilder("java", "-jar", jar.absolutePath)
//            .inheritIO()
//            .start()
//    }
//}

tasks.register<Delete>("cleanKotlinMQTTServers") { //Delete temp MQTT files before starting the server
    val baseDir = rootProject.projectDir

    delete(
        provider {
            baseDir.listFiles()
                ?.filter { it.isDirectory && it.name.startsWith("KotlinMQTTServer") }
                ?: emptyList()
        }
    )
}
/*
fun Project.runCommand(command: String): String {
    val output = ByteArrayOutputStream()
    exec {
        commandLine("bash", "-c", command)
        standardOutput = output
        errorOutput = output
        isIgnoreExitValue = true
    }
    return output.toString().trim()
}

tasks.register("ensureDockerComposeAndWait") {
    doLast {
        println("🐳 Checking docker compose...")

        val runningContainers = project.runCommand("docker compose ps -q")

        if (runningContainers.isEmpty()) {
            println("🚀 Starting docker compose...")
            project.runCommand("docker compose up -d")
        }

        val maxRetries = 30
        val waitSeconds = 2

        println("⏳ Waiting for Postgres...")

        repeat(maxRetries) { attempt ->
            val result = project.runCommand("nc -z localhost 5432")

            if (result.isEmpty()) {
                println("✅ Postgres is ready!")
                return@doLast
            }

            println("⌛ Attempt ${attempt + 1}/$maxRetries...")
            Thread.sleep(waitSeconds * 1000L)
        }

        println("❌ Timeout waiting for Postgres")
        exitProcess(1)
    }
}*/


// ./gradlew generateJooq
jooq {
    edition.set(JooqEdition.OSS)

    configurations {
        create("main") {
            jooqConfiguration.apply {
                logging = Logging.WARN

                jdbc = Jdbc().apply {
                    driver = "org.postgresql.Driver"

                    url = System.getenv("CRONIOT_DB_URL") 
                        ?: localProperties.getProperty("CRONIOT_DB_URL") 
                        ?: project.findProperty("CRONIOT_DB_URL")?.toString()
                        
                    user = System.getenv("CRONIOT_DB_USER") 
                        ?: localProperties.getProperty("CRONIOT_DB_USER") 
                        ?: project.findProperty("CRONIOT_DB_USER")?.toString()
                        
                    password = System.getenv("CRONIOT_DB_PASSWORD") 
                        ?: localProperties.getProperty("CRONIOT_DB_PASSWORD") 
                        ?: project.findProperty("CRONIOT_DB_PASSWORD")?.toString()
                }

                generator = Generator().apply {
                    name = "org.jooq.codegen.KotlinGenerator"

                    database = Database().apply {
                        name = "org.jooq.meta.postgres.PostgresDatabase"
                        inputSchema = "public" // o tu schema real
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
