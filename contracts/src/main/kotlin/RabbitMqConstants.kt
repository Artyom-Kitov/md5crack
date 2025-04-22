package ru.nsu.dsi.md5

const val REQUEST_QUEUE = "to_workers_queue"
const val REQUEST_EXCHANGE = "to_workers_exchange"
const val REQUEST_KEY = "to_workers_key"

const val RESPONSE_QUEUE = "to_manager"
const val RESPONSE_EXCHANGE = "to_manager_exchange"
const val RESPONSE_KEY = "to_manager_key"

val REQUEST_OPTIONS = mapOf(
    "x-consumer-timeout" to 60_000,
    "x-dead-letter-exchange" to REQUEST_EXCHANGE,
)

val RESPONSE_OPTIONS = mapOf(
    "x-consumer-timeout" to 60_000,
    "x-dead-letter-exchange" to RESPONSE_EXCHANGE,
)
