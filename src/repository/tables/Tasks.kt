package io.defolters.repository.tables

import io.defolters.models.TaskStatus
import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val itemId = integer("itemId").references(Items.id, onDelete = ReferenceOption.CASCADE)
    val taskDependencyId = integer("taskDependencyId").references(id).nullable()
    val workerTypeId = integer("workerTypeId").references(WorkerTypes.id)
    val title = varchar("title", 512)
    val timeToComplete = integer("timeToComplete")
    val isAdditional = bool("isAdditional")
    val status = enumerationByName("status", 10, TaskStatus::class)

    override val primaryKey = PrimaryKey(id)
}
