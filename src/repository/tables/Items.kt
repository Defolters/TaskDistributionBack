package io.defolters.repository.tables

import org.jetbrains.exposed.sql.Table

object Items : Table() {
    val id = integer("id").autoIncrement()
    val orderId = integer("orderId").references(Orders.id)
    val title = varchar("title", 512)
    val info = varchar("info", 512)
    val price = float("price")

    override val primaryKey = PrimaryKey(id)
}