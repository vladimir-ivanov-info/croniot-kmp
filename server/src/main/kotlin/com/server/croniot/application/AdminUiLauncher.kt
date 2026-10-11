package com.server.croniot.application

import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import kotlin.concurrent.thread
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Dev convenience: launches the admin-ui Vite dev server alongside the backend, the same way
 * [ensureDockerComposeRunning] brings up Postgres. Never fails the server boot: if admin-ui
 * or Node is missing it only logs a warning.
 *
 * Env vars: `CRONIOT_ADMIN_UI=false` disables it, `CRONIOT_ADMIN_UI_DIR` overrides the
 * folder (default `../admin-ui`), `CRONIOT_NODE_BIN` overrides the Node bin dir.
 */
object AdminUiLauncher {

    private val logger = KotlinLogging.logger {}

    private const val PORT = 5173
    private const val URL = "http://localhost:$PORT"
    private const val PORT_CHECK_TIMEOUT_MS = 200

    private var process: Process? = null

    fun start() {
        val adminUiDir = adminUiDirToLaunch() ?: return
        launch(adminUiDir)
    }

    fun stop() {
        // npm spawns vite as a child process; destroying only npm would leave vite holding the port
        process?.let { devServer ->
            devServer.descendants().forEach { it.destroy() }
            devServer.destroy()
        }
    }

    // Returns null (and logs why) when there's nothing to launch
    private fun adminUiDirToLaunch(): File? {
        val adminUiDir = System.getenv("CRONIOT_ADMIN_UI_DIR")?.let(::File)
            ?: File(System.getProperty("user.dir"), "../admin-ui")

        when {
            System.getenv("CRONIOT_ADMIN_UI").equals("false", ignoreCase = true) ->
                logger.info { "Admin UI auto-start disabled (CRONIOT_ADMIN_UI=false)" }
            !File(adminUiDir, "package.json").isFile ->
                logger.warn { "Admin UI not found at ${adminUiDir.canonicalPath}, skipping" }
            isLocalPortInUse(PORT) ->
                logger.info { "Admin UI already running: $URL" }
            else -> return adminUiDir
        }
        return null
    }

    private fun launch(adminUiDir: File) {
        val nodeBinDir = resolveNodeBinDir()
        val npm = nodeBinDir?.let { File(it, "npm").path } ?: "npm"
        logger.info { "Using Node from ${nodeBinDir?.path ?: "PATH"}" }
        val logFile = File(System.getProperty("user.dir"), "logs/admin-ui.log").apply { parentFile.mkdirs() }

        try {
            if (!File(adminUiDir, "node_modules").isDirectory) {
                logger.info { "Installing Admin UI dependencies (npm ci)..." }
                val exitCode = ProcessBuilder(npm, "ci")
                    .withNodeOnPath(nodeBinDir)
                    .directory(adminUiDir)
                    .redirectErrorStream(true)
                    .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                    .start()
                    .waitFor()
                if (exitCode != 0) {
                    logger.warn { "npm ci failed (exit code: $exitCode), see ${logFile.path}" }
                    return
                }
            }

            val devServer = ProcessBuilder(npm, "run", "dev", "--", "--port", "$PORT", "--strictPort")
                .withNodeOnPath(nodeBinDir)
                .directory(adminUiDir)
                .redirectErrorStream(true)
                .redirectOutput(ProcessBuilder.Redirect.appendTo(logFile))
                .start()
            process = devServer
            logger.info { "Starting Admin UI (logs: ${logFile.path})" }
            logWhenReady(devServer, logFile)
        } catch (e: IOException) {
            logger.warn(e) { "Could not start Admin UI (is npm on PATH? otherwise set CRONIOT_NODE_BIN)" }
        }
    }

    // The IDE doesn't load nvm, so PATH may only have the system Node (too old for Vite).
    // Order: CRONIOT_NODE_BIN, then the newest nvm-installed Node, then whatever is on PATH.
    private fun resolveNodeBinDir(): File? {
        System.getenv("CRONIOT_NODE_BIN")?.let { return File(it) }

        val nvmDir = System.getenv("NVM_DIR")?.let(::File) ?: File(System.getProperty("user.home"), ".nvm")
        return File(nvmDir, "versions/node").listFiles()
            ?.filter { File(it, "bin/npm").canExecute() }
            ?.maxWithOrNull { a, b -> compareVersions(a.name, b.name) }
            ?.let { File(it, "bin") }
    }

    private fun compareVersions(a: String, b: String): Int {
        val partsA = a.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        val partsB = b.removePrefix("v").split('.').map { it.toIntOrNull() ?: 0 }
        return partsA.zip(partsB).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 } ?: 0
    }

    // npm's shebang is `#!/usr/bin/env node`, so node must come first on PATH too, not just npm
    private fun ProcessBuilder.withNodeOnPath(nodeBinDir: File?): ProcessBuilder = apply {
        if (nodeBinDir != null) {
            environment()["PATH"] = nodeBinDir.path + File.pathSeparator + environment()["PATH"].orEmpty()
        }
    }

    // Runs in the background so the server boot isn't blocked waiting for vite
    private fun logWhenReady(
        devServer: Process,
        logFile: File,
        timeout: Duration = 60.seconds,
        pollInterval: Duration = 500.milliseconds,
    ) = thread(isDaemon = true, name = "admin-ui-readiness") {
        val deadline = System.currentTimeMillis() + timeout.inWholeMilliseconds

        while (System.currentTimeMillis() < deadline) {
            if (!devServer.isAlive) {
                logger.warn { "Admin UI exited (exit code: ${devServer.exitValue()}), see ${logFile.path}" }
                return@thread
            }
            if (isLocalPortInUse(PORT)) {
                logger.info { "Admin UI ready: $URL" }
                return@thread
            }
            Thread.sleep(pollInterval.inWholeMilliseconds)
        }

        logger.warn { "Admin UI not ready after ${timeout.inWholeSeconds}s, see ${logFile.path}" }
    }

    private fun isLocalPortInUse(port: Int): Boolean =
        // vite may bind to ::1 or 127.0.0.1 depending on how "localhost" resolves
        InetAddress.getAllByName("localhost").any { address ->
            runCatching {
                Socket().use { it.connect(InetSocketAddress(address, port), PORT_CHECK_TIMEOUT_MS) }
            }.isSuccess
        }
}
