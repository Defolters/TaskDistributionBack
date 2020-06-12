package io.defolters.repository.tables

import org.jetbrains.exposed.sql.Table

object WorkerTypes : Table() {
    val id = integer("id").autoIncrement()
    val title = varchar("title", 512)
    val isActive = bool("isActive")

    override val primaryKey = PrimaryKey(id)
}
