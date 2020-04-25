package io.defolters.models

data class Task(
    val id: Int,
    val itemId: Int,
    val taskDependencyId: Int?,
    val workerTypeId: Int,
//    val workerId: Int,
    val title: String,
    val timeToComplete: Int,
    val status: TaskStatus
)
