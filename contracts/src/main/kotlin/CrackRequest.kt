package ru.nsu.dsi.md5

import kotlinx.serialization.Serializable

@Serializable
data class CrackRequest(
    val hash: String,
    val maxLen: Int,
)
