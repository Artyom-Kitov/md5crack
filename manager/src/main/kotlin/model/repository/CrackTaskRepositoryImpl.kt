package ru.nsu.dsi.md5.model.repository

import io.ktor.server.config.*
import org.litote.kmongo.KMongo
import org.litote.kmongo.deleteOneById
import org.litote.kmongo.eq
import org.litote.kmongo.findOneById
import org.litote.kmongo.getCollection
import org.litote.kmongo.updateOneById

class CrackTaskRepositoryImpl(
    config: ApplicationConfig,
) : CrackTaskRepository {
    private val mongoUri = config.property("ktor.application.database.connection").getString()
    private val databaseName = config.property("ktor.application.database.name").getString()
    private val client = KMongo.createClient(mongoUri)
    private val database = client.getDatabase(databaseName)

    private val tasks = database.getCollection<CrackTask>("tasks")

    override fun addTask(task: CrackTask): Boolean {
        return tasks.insertOne(task)
            .wasAcknowledged()
    }

    override fun findById(id: String): CrackTask? {
        return tasks.findOneById(id)
    }

    override fun updateById(id: String, newTask: CrackTask): Boolean {
        return tasks.updateOneById(id, newTask).wasAcknowledged()
    }

    override fun removeById(id: String) {
        tasks.deleteOneById(id)
    }

    override fun findAllByStatus(status: TaskStatus): List<CrackTask> {
        return tasks.find(CrackTask::status eq status).toList()
    }
}