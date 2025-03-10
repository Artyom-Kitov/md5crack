package ru.nsu.dsi.md5.routing

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import ru.nsu.dsi.md5.WorkerCrackRequest
import ru.nsu.dsi.md5.model.Md5CrackService

fun Application.configureRouting() {
    val md5CrackService = Md5CrackService(environment.config, log)

    routing {
        post("/internal/api/worker/hash/crack/task") {
            val request = call.receive<WorkerCrackRequest>()
            call.respond(HttpStatusCode.OK)

            launch {
                md5CrackService.startCrack(request)
            }
        }
    }
}
