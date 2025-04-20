package ru.nsu.dsi.md5.model

import com.rabbitmq.client.AMQP
import io.ktor.server.config.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
import ru.nsu.dsi.md5.RESPONSE_EXCHANGE
import ru.nsu.dsi.md5.RESPONSE_KEY
import ru.nsu.dsi.md5.WorkerCrackRequest
import ru.nsu.dsi.md5.WorkerCrackResult

class Md5CrackService(
    config: ApplicationConfig,
    private val log: Logger,
    private val channelProvider: ToManagerChannelProvider,
) {
    private val workerId = config.property("ktor.application.worker-id").getString().toInt()

    fun startCrack(request: WorkerCrackRequest) {
        log.info("Starting crack {}", request)
        val cracked = Md5Cracker(ALPHABET, request.hash).crack(
            partNum = request.partNum,
            partCount = request.partCount,
            maxLen = request.maxLen,
        )
        log.info("Successfully cracked {}, result: {}", request, cracked)
        sendToManager(request.requestId, cracked)
    }

    private fun sendToManager(requestId: String, cracked: List<String>) {
        log.info("Sending result to manager")
        try {
            channelProvider.buildChannel().use { channel ->
                val result = WorkerCrackResult(workerId, requestId, cracked)
                val bytes = Json.encodeToString(result).encodeToByteArray()
                channel.basicPublish(RESPONSE_EXCHANGE, RESPONSE_KEY, MESSAGE_PROPERTIES, bytes)
            }
            log.info("Sent to manager")
        } catch (e: Exception) {
            log.error(e)
        }
    }

    companion object {
        private val ALPHABET = setOf(
            CharRange('a', 'z').toSet(),
            CharRange('0', '9').toSet(),
            setOf(' ')
        ).flatten().toSet()

        private val MESSAGE_PROPERTIES = AMQP.BasicProperties.Builder()
                .deliveryMode(2)
                .build()
    }
}