package io.defolters.repository.tables

import io.defolters.models.UserType
import org.jetbrains.exposed.sql.Table

object Users : Table() {
    val userId = integer("id").autoIncrement()
    val email = varchar("email", 128).uniqueIndex()
    val displayName = varchar("displayName", 256)
    val passwordHash = varchar("passwordHash", 64)
    val userType = enumerationByName("userType", 10, UserType::class)
    val workerTypeId = integer("workerTypeId").references(WorkerTypes.id).nullable()

    override val primaryKey = PrimaryKey(userId)
}