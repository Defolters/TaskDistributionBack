package io.defolters.repository.tables

import org.jetbrains.exposed.sql.ReferenceOption
import org.jetbrains.exposed.sql.Table

object Items : Table() {
    val id = integer("id").autoIncrement()
    val orderId = integer("orderId").references(Orders.id, onDelete = ReferenceOption.CASCADE)
    val title = varchar("title", 512)
    val info = varchar("info", 512)
    val price = double("price")
    val isReady = bool("isReady")
    val color = varchar("color", 10)

    override val primaryKey = PrimaryKey(id)
}
