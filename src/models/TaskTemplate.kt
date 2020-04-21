package io.defolters.models

data class TaskTemplate(
    val id: Int,
    val title: String,
    val itemTemplateId: Int,
    val taskTemplateDependencyId: Int?,
    val workerTypeId: Int,
    val timeToComplete: Int,
    val isAdditional: Boolean //val type = enumerationByName("type", 10, Type::class.java)
)
