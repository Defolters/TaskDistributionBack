package io.defolters.repository.interfaces

import io.defolters.models.TaskTemplate

interface TaskTemplateRepository : RepositoryInterface {
    suspend fun addTaskTemplate(
        title: String,
        itemTemplateId: Int,
        taskTemplateDependencyId: Int?,
        workerTypeId: Int,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate?

    suspend fun deleteTaskTemplate(id: Int)
    suspend fun getTaskTemplates(): List<TaskTemplate>
    suspend fun getTaskTemplates(itemTemplateId: Int): List<TaskTemplate>
    suspend fun getTaskTemplates(itemTemplateId: Int, isAdditional: Boolean): List<TaskTemplate>
    suspend fun findTaskTemplate(id: Int): TaskTemplate?
    suspend fun updateTaskTemplate(
        id: Int,
        title: String,
        itemTemplateId: Int,
        taskTemplateDependencyId: Int?,
        workerTypeId: Int,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate?
}