package ru.nsu.dsi.md5.model.repository

import org.bson.codecs.pojo.annotations.BsonId
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

enum class TaskStatus {
    NEW,
    IN_PROGRESS,
    READY,
}

data class CrackTask @OptIn(ExperimentalUuidApi::class) constructor(
    @BsonId val id: String = Uuid.random().toString(),
    val data: MutableSet<String>,
    val remaining: MutableSet<Int>,
    val status: TaskStatus,
)