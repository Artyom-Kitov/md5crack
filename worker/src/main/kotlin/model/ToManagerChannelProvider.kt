package ru.nsu.dsi.md5.model

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.config.*
import ru.nsu.dsi.md5.RESPONSE_EXCHANGE
import ru.nsu.dsi.md5.RESPONSE_KEY
import ru.nsu.dsi.md5.RESPONSE_QUEUE

class ToManagerChannelProvider(
    config: ApplicationConfig,
) {
    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.setUri(config.property("ktor.application.rabbitmq.uri").getString())
    }

    fun buildChannel(): Channel {
        val connection = connectionFactory.newConnection()
        val channel = connection.createChannel()
        channel.exchangeDeclare(RESPONSE_EXCHANGE, BuiltinExchangeType.DIRECT, true)
        channel.queueDeclare(RESPONSE_QUEUE, true, false, false, mapOf())
        channel.queueBind(RESPONSE_QUEUE, RESPONSE_EXCHANGE, RESPONSE_KEY)
        return channel
    }
}