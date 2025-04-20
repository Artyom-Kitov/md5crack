package ru.nsu.dsi.md5.model.repository

interface CrackTaskRepository {
    fun addTask(task: CrackTask): Boolean

    fun findById(id: String): CrackTask?

    fun updateById(id: String, newTask: CrackTask): Boolean

    fun removeById(id: String)
}