package io.defolters.repository.tables

import org.jetbrains.exposed.sql.Table

object ScheduleTasks : Table() {
    val id = integer("id").autoIncrement()
    val workerTypeId = integer("workerTypeId").references(WorkerTypes.id)
    val title = varchar("title", 512)
    val start = varchar("start", 512)
    val end = varchar("end", 512)
    val color = varchar("color", 10)

    override val primaryKey = PrimaryKey(id)
}
