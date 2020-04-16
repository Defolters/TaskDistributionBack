package io.defolters.repository.interfaces

import io.defolters.models.Task

interface TaskRepository : RepositoryInterface {
    suspend fun getTasks(): List<Task>
    suspend fun getTasks(itemId: Int): List<Task>
}