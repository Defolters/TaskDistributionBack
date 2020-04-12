package io.defolters.models

data class TaskTemplate(
    val id: Int,
    val title: String,
    val itemTemplateId: Int,
    val taskTemplateDependencyId: Int?,
    val workerType: String,
    val timeToComplete: Int,
    val isAdditional: Boolean //val type = enumerationByName("type", 10, Type::class.java)
)
