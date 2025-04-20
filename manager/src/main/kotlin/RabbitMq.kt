package ru.nsu.dsi.md5

import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.ktor.server.application.*

fun Application.configureRabbitMq() {
    install(RabbitMQ) {
        uri = environment.config.property("ktor.application.rabbitmq.uri").getString()
        defaultConnectionName = "manager"
        connectionAttempts = 10
        attemptDelay = 10
        dispatcherThreadPollSize = 2
        tlsEnabled = false
    }
}
