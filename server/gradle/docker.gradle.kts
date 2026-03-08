//import java.io.ByteArrayOutputStream
//import kotlin.system.exitProcess
//
//val DOCKER_PORT = "5433"
//
//fun Project.runCommand(command: String): String {
//    val output = ByteArrayOutputStream()
//    exec {
//        commandLine("bash", "-c", command)
//        standardOutput = output
//        errorOutput = output
//        isIgnoreExitValue = true
//    }
//    return output.toString().trim()
//}
//
//tasks.register("ensureDockerComposeRunning") {
//    group = "docker"
//    description = "Ensure docker compose containers are running"
//
//    doLast {
//        println("🐳 Checking docker compose containers...")
//
//        val containers = project.runCommand("docker compose ps -q")
//
//        if (containers.isEmpty()) {
//            println("🚀 Docker compose is NOT running. Starting it...")
//            project.runCommand("docker compose up -d")
//        } else {
//            println("✅ Docker compose already running")
//        }
//    }
//}
