package ru.nsu.dsi.md5.model

import com.rabbitmq.client.BuiltinExchangeType
import com.rabbitmq.client.Channel
import com.rabbitmq.client.ConnectionFactory
import io.ktor.server.config.*
import ru.nsu.dsi.md5.REQUEST_EXCHANGE
import ru.nsu.dsi.md5.REQUEST_KEY
import ru.nsu.dsi.md5.REQUEST_QUEUE

class ToWorkersChannelProvider(
    config: ApplicationConfig,
) {
    private val connectionFactory = ConnectionFactory()

    init {
        connectionFactory.setUri(config.property("ktor.application.rabbitmq.uri").getString())
    }

    fun buildChannel(): Channel {
        val connection = connectionFactory.newConnection()
        val channel = connection.createChannel()
        channel.exchangeDeclare(REQUEST_EXCHANGE, BuiltinExchangeType.DIRECT, true)
        channel.queueDeclare(REQUEST_QUEUE, true, false, false, mapOf())
        channel.queueBind(REQUEST_QUEUE, REQUEST_EXCHANGE, REQUEST_KEY)
        return channel
    }
}