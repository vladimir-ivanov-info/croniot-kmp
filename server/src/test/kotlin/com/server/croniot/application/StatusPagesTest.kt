package com.server.croniot.application

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import croniot.messages.MessageFactory
import croniot.models.errors.DomainError
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.ktor.util.cio.ChannelWriteException
import io.ktor.utils.io.ClosedWriteChannelException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class StatusPagesTest {

    private val logger = LoggerFactory.getLogger("StatusPages") as Logger
    private val appender = ListAppender<ILoggingEvent>()
    private var previousLevel: Level? = null

    @BeforeEach
    fun attachAppender() {
        previousLevel = logger.level
        logger.level = Level.DEBUG
        appender.start()
        logger.addAppender(appender)
    }

    @AfterEach
    fun detachAppender() {
        logger.detachAppender(appender)
        appender.stop()
        logger.level = previousLevel
    }

    private fun Application.testModule(failure: () -> Throwable) {
        install(ContentNegotiation) { json(MessageFactory.json) }
        installStatusPages()
        routing {
            get("/boom") { throw failure() }
        }
    }

    private fun errorsLogged() = appender.list.filter { it.level == Level.ERROR }

    @Test
    fun `a DomainException is answered with its mapped status and not logged as an error`() = testApplication {
        application { testModule { DomainException(DomainError.NotFound("account")) } }

        // when
        val response = client.get("/boom")

        // then
        assertEquals(HttpStatusCode.NotFound, response.status)
        assertTrue(response.bodyAsText().contains(DomainError.NotFound.CODE))
        assertTrue(errorsLogged().isEmpty())
    }

    @Test
    fun `an unexpected exception is logged as an error and answered with a generic 500`() = testApplication {
        application { testModule { IllegalStateException("secret detail") } }

        // when
        val response = client.get("/boom")

        // then
        assertEquals(HttpStatusCode.InternalServerError, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(DomainError.Internal.CODE))
        assertFalse(body.contains("secret detail"))
        assertEquals(1, errorsLogged().size)
    }

    @Test
    fun `a ChannelWriteException is a client disconnect and is not logged as an error`() = testApplication {
        application { testModule { ChannelWriteException(exception = RuntimeException("closed")) } }

        // when
        client.get("/boom")

        // then
        assertTrue(errorsLogged().isEmpty())
        assertTrue(appender.list.any { it.level == Level.DEBUG && it.formattedMessage.contains("/boom") })
    }

    @Test
    fun `a ClosedWriteChannelException is a client disconnect and is not logged as an error`() = testApplication {
        application { testModule { ClosedWriteChannelException(RuntimeException("closed")) } }

        // when
        client.get("/boom")

        // then
        assertTrue(errorsLogged().isEmpty())
        assertTrue(appender.list.any { it.level == Level.DEBUG && it.formattedMessage.contains("/boom") })
    }
}
