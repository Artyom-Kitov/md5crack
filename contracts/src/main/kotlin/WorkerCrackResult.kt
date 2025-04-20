package ru.nsu.dsi.md5

import kotlinx.serialization.Serializable

@Serializable
data class WorkerCrackResult(
    val workerId: Int,
    val requestId: String,
    val data: List<String>,
)
