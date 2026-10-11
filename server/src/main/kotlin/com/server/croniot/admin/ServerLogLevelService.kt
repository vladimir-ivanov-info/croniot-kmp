package com.server.croniot.admin

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.LoggerContext
import com.server.croniot.application.ApplicationScope
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@Serializable
data class ServerLogLevelDto(
    // Effective level of the root logger (what applies to everything not
    // overridden below).
    val root: String,
    // Loggers changed through this API that are still in effect.
    val overrides: Map<String, String>,
    // When the temporary change reverts, if one is active.
    val revertsAtMs: Long?,
)

@Serializable
data class SetServerLogLevelRequest(
    val level: String,
    // Logger name; null or "ROOT" means the root logger.
    val logger: String? = null,
    // How long it lasts. Default 30 min, so DEBUG is never left on by accident.
    val ttlMinutes: Long? = null,
)

// Changes Logback levels at runtime, without a restart, and puts them back
// when the TTL runs out. Lines at the new level flow to server.log,
// server_log and the live viewer like any other.
@Singleton
class ServerLogLevelService(
    private val context: LoggerContext,
    private val scope: CoroutineScope,
    private val clock: () -> Long,
) {
    @Inject constructor(scope: ApplicationScope) :
        this(LoggerFactory.getILoggerFactory() as LoggerContext, scope, System::currentTimeMillis)

    private val logger = KotlinLogging.logger {}

    // Original level of every logger changed, to restore on revert.
    private val originals = LinkedHashMap<String, Level?>()
    private var revertJob: Job? = null
    private var revertsAtMs: Long? = null

    @Synchronized
    fun current(): ServerLogLevelDto = ServerLogLevelDto(
        root = context.getLogger(Logger.ROOT_LOGGER_NAME).effectiveLevel.levelStr,
        overrides = originals.keys.associateWith { context.getLogger(it).effectiveLevel.levelStr },
        revertsAtMs = revertsAtMs,
    )

    @Synchronized
    fun set(request: SetServerLogLevelRequest): ServerLogLevelDto {
        val level = parse(request.level) ?: throw IllegalArgumentException("Unknown level '${request.level}'")
        val name = request.logger?.takeUnless { it.isBlank() || it.equals("ROOT", ignoreCase = true) }
            ?: Logger.ROOT_LOGGER_NAME
        val target = context.getLogger(name)
        if (name !in originals) originals[name] = target.level
        target.level = level

        val ttl = (request.ttlMinutes ?: DEFAULT_TTL.inWholeMinutes).coerceIn(1, MAX_TTL.inWholeMinutes).minutes
        scheduleRevert(ttl)
        logger.warn { "Server log level of $name set to ${level.levelStr} for ${ttl.inWholeMinutes} min" }
        return current()
    }

    @Synchronized
    fun revert(): ServerLogLevelDto {
        revertJob?.cancel()
        revertJob = null
        revertsAtMs = null
        originals.forEach { (name, original) -> context.getLogger(name).level = original }
        if (originals.isNotEmpty()) logger.warn { "Server log levels restored: ${originals.keys.joinToString()}" }
        originals.clear()
        return current()
    }

    private fun scheduleRevert(ttl: Duration) {
        revertJob?.cancel()
        revertsAtMs = clock() + ttl.inWholeMilliseconds
        revertJob = scope.launch {
            delay(ttl)
            revert()
        }
    }

    companion object {
        val DEFAULT_TTL = 30.minutes
        val MAX_TTL = (24 * 60).minutes
        private val LEVELS = listOf("TRACE", "DEBUG", "INFO", "WARN", "ERROR")

        fun parse(raw: String): Level? = raw.uppercase().takeIf { it in LEVELS }?.let { Level.toLevel(it) }
    }
}
