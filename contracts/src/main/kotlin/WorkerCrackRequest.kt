package ru.nsu.dsi.md5

import kotlinx.serialization.Serializable

@Serializable
data class WorkerCrackRequest(
    val requestId: String,
    val hash: String,
    val maxLen: Int,
    val partNum: Int,
    val partCount: Int,
)