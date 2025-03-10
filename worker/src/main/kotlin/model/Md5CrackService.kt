package ru.nsu.dsi.md5.model

import io.ktor.client.*
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.config.*
import io.ktor.util.logging.*
import ru.nsu.dsi.md5.WorkerCrackRequest
import ru.nsu.dsi.md5.WorkerCrackResult

class Md5CrackService(
    config: ApplicationConfig,
    private val log: Logger,
) {
    private val managerHost = config.property("ktor.application.manager.host").getString()
    private val managerPort = config.property("ktor.application.manager.port").getString().toInt()
    private val url = "http://$managerHost:$managerPort/internal/api/manager/hash/crack/request"

    suspend fun startCrack(request: WorkerCrackRequest) {
        log.info("Starting crack {}", request)
        val cracked = Md5Cracker(ALPHABET, request.hash).crack(
            partNum = request.partNum,
            partCount = request.partCount,
            maxLen = request.maxLen,
        )
        log.info("Successfully cracked {}, result: {}", request, cracked)
        sendToManager(request.requestId, cracked)
    }

    private suspend fun sendToManager(requestId: String, cracked: List<String>) {
        log.info("Sending result to manager")
        try {
            val result = HttpClient(Apache5) {
                install(ContentNegotiation) {
                    json()
                }
            }.use { client ->
                client.patch(url) {
                    contentType(ContentType.Application.Json)
                    setBody(WorkerCrackResult(requestId, cracked))
                }
            }
            log.info("Response from manager: {}", result)
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
    }
}