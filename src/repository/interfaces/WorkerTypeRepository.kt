package io.defolters.repository.interfaces

import io.defolters.models.WorkerType

interface WorkerTypeRepository : RepositoryInterface {
    suspend fun addWorkerType(title: String): WorkerType?
    suspend fun deleteWorkerType(id: Int)
    suspend fun getWorkerTypes(): List<WorkerType>
    suspend fun findWorkerType(id: Int?): WorkerType?
    suspend fun updateWorkerType(id: Int, title: String): WorkerType?
}