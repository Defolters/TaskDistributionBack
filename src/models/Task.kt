package io.defolters.models

data class Task(
    val id: Int,
    val itemId: Int,
//    val taskDependencyId: Int,
//    val workerId: Int,
    val title: String,
    val workerType: String,
    val timeToComplete: Int,
    val status: TaskStatus
)
