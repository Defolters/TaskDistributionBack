package io.defolters.repository.tables

import org.jetbrains.exposed.sql.Table

object Orders : Table() {
    val id = integer("id").autoIncrement()
    val customerName = varchar("customerName", 512)
    val customerEmail = varchar("customerEmail", 512)
    val price = double("price")
    val createdAt = varchar("createdAt", 128)
    val isReady = bool("isReady")

    override val primaryKey = PrimaryKey(id)
}
