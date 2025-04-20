package ru.nsu.dsi.md5

import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicAck
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.server.application.*
import ru.nsu.dsi.md5.model.Md5CrackService
import ru.nsu.dsi.md5.model.ToManagerChannelProvider

fun Application.configureRabbitMq() {
    val workerId = environment.config.property("ktor.application.worker-id").getString().toInt()
    install(RabbitMQ) {
        uri = environment.config.property("ktor.application.rabbitmq.uri").getString()
        defaultConnectionName = "worker-$workerId"
        connectionAttempts = 10
        attemptDelay = 10
        dispatcherThreadPollSize = 2
        tlsEnabled = false
    }

    val channelProvider = ToManagerChannelProvider(environment.config)
    val md5CrackService = Md5CrackService(environment.config, log, channelProvider)
    rabbitmq {
        basicConsume {
            autoAck = false
            queue = REQUEST_QUEUE
            deliverCallback<WorkerCrackRequest> { tag, request ->
                md5CrackService.startCrack(request)
                basicAck {
                    deliveryTag = tag
                    multiple = false
                }
            }
        }
    }
}
