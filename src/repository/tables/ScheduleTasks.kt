package io.defolters.repository.tables

import io.defolters.models.TaskStatus
import org.jetbrains.exposed.sql.Table

object ScheduleTasks : Table() {
    val id = integer("id").autoIncrement()
    val workerTypeId = integer("workerTypeId").references(WorkerTypes.id)
    val itemId = integer("itemId")
    val taskId = integer("taskId")
    val taskDependencyId = integer("taskDependencyId").nullable()
    val taskStatus = enumerationByName("taskStatus", 10, TaskStatus::class)
    val title = varchar("title", 512)
    val start = varchar("start", 512)
    val end = varchar("end", 512)
    val color = varchar("color", 10)

    override val primaryKey = PrimaryKey(id)
}
