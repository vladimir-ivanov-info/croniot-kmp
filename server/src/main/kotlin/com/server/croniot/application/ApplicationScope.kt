package com.server.croniot.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

@Singleton
class ApplicationScope @Inject constructor() : CoroutineScope {

    private val logger = KotlinLogging.logger {}

    private val exceptionHandler = CoroutineExceptionHandler { context, throwable ->
        logger.error(throwable) { "Uncaught exception in ApplicationScope (context=$context)" }
    }

    override val coroutineContext: CoroutineContext =
        SupervisorJob() + Dispatchers.IO + exceptionHandler

    fun shutdown() {
        cancel()
    }
}