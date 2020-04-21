package io.defolters.repository.tables

import org.jetbrains.exposed.sql.Table

object TaskTemplates : Table() {
    val id = integer("id").autoIncrement()
    val itemTemplateId = integer("itemTemplateId").references(ItemTemplates.id)
    val taskTemplateDependencyId = integer("taskTemplateDependencyId").references(id).nullable()
    val workerTypeId = integer("workerTypeId").references(WorkerTypes.id)
    val title = varchar("title", 512)
    val timeToComplete = integer("timeToComplete")
    val isAdditional = bool("isAdditional")

    override val primaryKey = PrimaryKey(id)
}
