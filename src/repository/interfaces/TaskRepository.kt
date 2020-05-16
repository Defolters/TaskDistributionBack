package io.defolters.repository.interfaces

import io.defolters.models.Task
import io.defolters.models.TaskStatus

interface TaskRepository : RepositoryInterface {
    suspend fun getTasks(): List<Task>
    suspend fun getItemTasks(itemId: Int): List<Task>
    suspend fun getWorkerTasks(workerTypeId: Int): List<Task>
    suspend fun findTask(id: Int?): Task?
    suspend fun updateTaskStatus(id: Int, status: TaskStatus): Task?
}