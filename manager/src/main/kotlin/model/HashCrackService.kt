package ru.nsu.dsi.md5.model

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import io.ktor.server.config.*
import io.ktor.util.logging.*
import kotlinx.serialization.json.Json
import ru.nsu.dsi.md5.CrackRequest
import ru.nsu.dsi.md5.CrackResponse
import ru.nsu.dsi.md5.CrackStatus
import ru.nsu.dsi.md5.REQUEST_EXCHANGE
import ru.nsu.dsi.md5.REQUEST_KEY
import ru.nsu.dsi.md5.WorkerCrackRequest
import ru.nsu.dsi.md5.WorkerCrackResult
import ru.nsu.dsi.md5.model.repository.CrackTask
import ru.nsu.dsi.md5.model.repository.CrackTaskRepository
import ru.nsu.dsi.md5.model.repository.TaskStatus
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class HashCrackService(
    config: ApplicationConfig,
    private val log: Logger,
    private val crackTaskRepository: CrackTaskRepository,
) {
    private val workers = config.property("ktor.application.manager.workers").getString().toInt()

    private val channelProvider = ToWorkersChannelProvider(config)

    /**
     * @return null if internal error happened
     */
    @OptIn(ExperimentalUuidApi::class)
    fun startCrack(request: CrackRequest): CrackResponse {
        val id = Uuid.random().toString()
        var task = CrackTask(
            id = id,
            hash = request.hash,
            maxLen = request.maxLen,
            data = mutableSetOf(),
            remaining = (0..<workers).toMutableSet(),
            status = TaskStatus.NEW
        )
        crackTaskRepository.addTask(task)

        return CrackResponse(id)
    }

    private fun sentCrackRequest(t: CrackTask) {
        var task = t.copy()
        try {
            log.info("Starting cracking hash = ${task.hash}")
            channelProvider.buildChannel().use { channel ->
                sendToWorkers(channel, task.id, CrackRequest(task.hash, task.maxLen))
            }
            log.info("Successfully started cracking hash = ${task.hash} by id = ${task.id}")
            task = task.copy(status = TaskStatus.IN_PROGRESS)
            crackTaskRepository.updateById(task.id, task)
        } catch (e: Exception) {
            log.error(e)
        }
    }

    private fun sendToWorkers(channel: Channel, id: String, fromClient: CrackRequest) {
        for (part in 0..<workers) {
            val request = WorkerCrackRequest(
                requestId = id,
                hash = fromClient.hash,
                maxLen = fromClient.maxLen,
                partNum = part,
                partCount = workers,
            )
            val bytes = Json.encodeToString(request).encodeToByteArray()
            channel.basicPublish(REQUEST_EXCHANGE, REQUEST_KEY, MESSAGE_PROPERTIES, bytes)
        }
    }

    /**
     * @return null if no request with such ID
     */
    fun getCrackStatus(id: String): CrackStatus? {
        val task = crackTaskRepository.findById(id) ?: return null
        return CrackStatus(
            status = task.status.toString(),
            data = task.data.toList(),
        )
    }

    /**
     * @return null if not request with such ID
     */
    fun addCracked(request: WorkerCrackResult): Unit? {
        log.info("Received crack result: ID = ${request.requestId}, worker = #${request.workerId}")
        var task = crackTaskRepository.findById(request.requestId) ?: return null
        task.data.addAll(request.data)
        if (request.workerId in task.remaining) {
            task.remaining.remove(request.workerId)
            task = task.copy(status = TaskStatus.READY)
            log.info("Request with ID = ${request.requestId} done successfully")
        }
        crackTaskRepository.updateById(task.id, task)
        return Unit
    }

    fun retryPending() {
        crackTaskRepository.findAllByStatus(TaskStatus.NEW).forEach(::sentCrackRequest)
    }

    companion object {
        private val MESSAGE_PROPERTIES: AMQP.BasicProperties = AMQP.BasicProperties.Builder()
            .deliveryMode(2)
            .build()
    }
}