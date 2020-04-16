package io.defolters.repository.tables

import io.defolters.models.TaskStatus
import org.jetbrains.exposed.sql.Table

object Tasks : Table() {
    val id = integer("id").autoIncrement()
    val itemId = integer("itemId").references(Items.id)
    val title = varchar("title", 512)
    val workerType = varchar("workerType", 100)
    val timeToComplete = integer("timeToComplete")
    val status = enumerationByName("status", 10, TaskStatus::class)

    override val primaryKey = PrimaryKey(id)
}
