package io.defolters.models

data class Task(
    val id: Int,
    val itemId: Int,
    val taskDependencyId: Int?,
    val workerTypeId: Int,
    val title: String,
    val timeToComplete: Int,
    val isAdditional: Boolean,
    val status: TaskStatus,
    val lastStatusUpdate: String,
    val isActive: Boolean
)
