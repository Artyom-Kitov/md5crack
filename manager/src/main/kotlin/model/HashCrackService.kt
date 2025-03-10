package ru.nsu.dsi.md5.model

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ru.nsu.dsi.md5.CrackRequest
import ru.nsu.dsi.md5.CrackResponse
import ru.nsu.dsi.md5.CrackStatus
import ru.nsu.dsi.md5.WorkerCrackRequest
import ru.nsu.dsi.md5.WorkerCrackResult
import java.util.concurrent.ConcurrentHashMap
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class HashCrackService(
    config: ApplicationConfig,
    private val log: Logger,
) {
    private val workerHost = config.property("ktor.application.manager.worker.host").getString()
    private val workerPort = config.property("ktor.application.manager.worker.port").getString().toInt()
    private val formatUrl = "http://$workerHost-%d:$workerPort/internal/api/worker/hash/crack/task"

    private val workers = config.property("ktor.application.manager.workers").getString().toInt()

    private data class CrackEntry(
        val data: MutableList<String>,
        var counter: Int,
    )

    private val requests: MutableMap<String, CrackEntry> = ConcurrentHashMap<String, CrackEntry>()

    /**
     * @return null if internal error happened
     */
    @OptIn(ExperimentalUuidApi::class)
    fun startCrack(request: CrackRequest): CrackResponse? {
        val id = Uuid.random().toString()
        requests[id] = CrackEntry(mutableListOf(), workers)
        return try {
            log.info("Starting cracking hash = ${request.hash}")
            HttpClient(Apache5) {
                install(ContentNegotiation) {
                    json()
                }
            }.use { client ->
                sendToWorkers(client, id, request)
            }
            log.info("Successfully started cracking hash = ${request.hash} by id = $id")
            CrackResponse(id)
        } catch (e: Exception) {
            log.error(e)
            requests.remove(id)
            null
        }
    }

    private fun sendToWorkers(client: HttpClient, id: String, fromClient: CrackRequest) = runBlocking {
        val tasks = mutableListOf<Job>()
        for (worker in 0..<workers) {
            val task = launch { sendToWorker(client, worker, id, fromClient) }
            tasks.add(task)
        }
        tasks.joinAll()
    }

    private suspend fun sendToWorker(client: HttpClient, worker: Int, id: String, fromClient: CrackRequest) {
        val request = WorkerCrackRequest(
            requestId = id,
            hash = fromClient.hash,
            maxLen = fromClient.maxLen,
            partNum = worker,
            partCount = workers,
        )
        val response = client.post(String.format(formatUrl, worker)) {
            contentType(ContentType.Application.Json)
            setBody(request)
        }
        log.info("Worker #{} response: {}", worker, response)
    }

    /**
     * @return null if no request with such ID
     */
    fun getCrackStatus(id: String) = synchronized(requests) {
        val entry = requests[id] ?: return@synchronized null
        val status = if (entry.counter != 0) "IN_PROGRESS" else "READY"
        CrackStatus(
            status = status,
            data = if (entry.counter == 0) entry.data else null,
        )
    }

    /**
     * @return null if not request with such ID
     */
    fun addCracked(request: WorkerCrackResult) = synchronized(requests) {
        val entry = requests[request.requestId] ?: return@synchronized null
        entry.data.addAll(request.data)
        entry.counter--
    }
}