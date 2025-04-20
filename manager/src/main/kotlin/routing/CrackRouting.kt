package ru.nsu.dsi.md5.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import ru.nsu.dsi.md5.CrackRequest
import ru.nsu.dsi.md5.WorkerCrackResult
import ru.nsu.dsi.md5.model.HashCrackService
import ru.nsu.dsi.md5.model.repository.CrackTaskRepositoryImpl

fun Application.crackRouting() {
    val crackTaskRepository = CrackTaskRepositoryImpl(environment.config)
    val hashCrackService = HashCrackService(environment.config, log, crackTaskRepository)

    routing {
        post("/api/hash/crack") {
            val request = call.receive<CrackRequest>()
            hashCrackService.startCrack(request).let {
                if (it != null) {
                    call.respond(HttpStatusCode.OK, it)
                } else {
                    call.respond(HttpStatusCode.InternalServerError)
                }
            }
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
        patch("/internal/api/manager/hash/crack/request") {
            val request = call.receive<WorkerCrackResult>()
            val code = if (hashCrackService.addCracked(request) != null) {
                HttpStatusCode.OK
            } else {
                HttpStatusCode.NotFound
            }
            call.respond(code)
        }
    }
}
