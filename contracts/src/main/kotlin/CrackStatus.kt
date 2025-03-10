package ru.nsu.dsi.md5

import kotlinx.serialization.Serializable

@Serializable
data class CrackStatus(
    val status: String,
    val data: List<String>?,
)