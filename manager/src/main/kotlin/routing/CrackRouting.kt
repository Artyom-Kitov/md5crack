package ru.nsu.dsi.md5.routing

import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.rabbitmq
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import ru.nsu.dsi.md5.CrackRequest
import ru.nsu.dsi.md5.RESPONSE_QUEUE
import ru.nsu.dsi.md5.WorkerCrackResult
import ru.nsu.dsi.md5.model.HashCrackService
import ru.nsu.dsi.md5.model.repository.CrackTaskRepositoryImpl

fun Application.crackRouting() {
    val crackTaskRepository = CrackTaskRepositoryImpl(environment.config)
    val hashCrackService = HashCrackService(environment.config, log, crackTaskRepository)
    val retryIntervalMillis = 15000L

    launch {
        while (isActive) {
            hashCrackService.retryPending()
            delay(retryIntervalMillis)
        }
    }

    routing {
        post("/api/hash/crack") {
            val request = call.receive<CrackRequest>()
            val response = hashCrackService.startCrack(request)
            call.respond(HttpStatusCode.OK, response)
        }
        get("/api/hash/status/{id}") {
            val id = call.parameters["id"]
            if (id == null) {
                call.respond(HttpStatusCode.BadRequest)
            } else {
                hashCrackService.getCrackStatus(id).let {
                    if (it != null) {
                        call.respond(HttpStatusCode.OK, it)
                    } else {
                        call.respond(HttpStatusCode.NotFound)
                    }
                }
            }
        }
    }
    rabbitmq {
        basicConsume {
            autoAck = true
            queue = RESPONSE_QUEUE
            deliverCallback<WorkerCrackResult> { _, result ->
                hashCrackService.addCracked(result)
            }
        }
    }
}
