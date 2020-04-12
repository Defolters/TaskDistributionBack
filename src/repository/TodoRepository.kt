package repository

import io.defolters.models.ItemTemplate
import io.defolters.models.TaskTemplate
import io.defolters.repository.tables.ItemTemplates
import io.defolters.repository.tables.TaskTemplates
import io.defolters.repository.tables.Todos
import io.defolters.repository.tables.Users
import models.Todo
import models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import repository.DatabaseFactory.dbQuery

class TodoRepository : Repository {

    override suspend fun addUser(email: String, displayName: String, passwordHash: String): User? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Users.insert {
                it[Users.email] = email
                it[Users.displayName] = displayName
                it[Users.passwordHash] = passwordHash
            }
        }
        return rowToUser(statement?.resultedValues?.get(0))
    }

    override suspend fun deleteUser(userId: Int) {
        dbQuery {
            Users.deleteWhere {
                Users.userId.eq(userId)
            }
        }
    }

    override suspend fun findUser(userId: Int) = dbQuery {
        Users.select { Users.userId.eq(userId) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = dbQuery {
        Users.select { Users.email.eq(email) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun addTodo(userId: Int, todo: String, done: Boolean): Todo? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Todos.insert {
                it[Todos.userId] = userId
                it[Todos.todo] = todo
                it[Todos.done] = done
            }
        }
        return rowToTodo(statement?.resultedValues?.get(0))
    }

    override suspend fun getTodos(userId: Int): List<Todo> {
        return dbQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.mapNotNull { rowToTodo(it) }
        }
    }

    override suspend fun getTodos(userId: Int, offset: Int, limit: Int): List<Todo> {
        return dbQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.limit(limit, offset = offset.toLong()).mapNotNull { rowToTodo(it) }
        }
    }

    override suspend fun deleteTodo(userId: Int, todoId: Int) {
        dbQuery {
            Todos.deleteWhere {
                Todos.id.eq(todoId)
                Todos.userId.eq(userId)
            }
        }
    }

    override suspend fun findTodo(userId: Int, todoId: Int): Todo? {
        return dbQuery {
            Todos.select {
                Todos.id.eq(todoId)
                Todos.userId.eq((userId))
            }.map { rowToTodo(it) }.singleOrNull()
        }
    }

    override suspend fun addItemTemplate(title: String): ItemTemplate? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = ItemTemplates.insert {
                it[ItemTemplates.title] = title
            }
        }
        return rowToItemTemplate(statement?.resultedValues?.get(0))
    }

    override suspend fun deleteItemTemplate(id: Int) {
        dbQuery {
            ItemTemplates.deleteWhere {
                ItemTemplates.id.eq(id)
            }
        }
    }

    override suspend fun getItemTemplates(): List<ItemTemplate> {
        return dbQuery {
            ItemTemplates.selectAll().mapNotNull { rowToItemTemplate(it) }
        }
    }

    override suspend fun addTaskTemplate(
        title: String,
        itemTemplateId: Int,
        taskTemplateDependencyId: Int?,
        workerType: String,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = TaskTemplates.insert {
                it[TaskTemplates.title] = title
                it[TaskTemplates.itemTemplateId] = itemTemplateId
                it[TaskTemplates.taskTemplateDependencyId] = taskTemplateDependencyId
                it[TaskTemplates.workerType] = workerType
                it[TaskTemplates.timeToComplete] = timeToComplete
                it[TaskTemplates.isAdditional] = isAdditional
            }
        }
        return rowToTaskTemplate(statement?.resultedValues?.get(0))
    }

    override suspend fun deleteTaskTemplate(id: Int) {
        dbQuery {
            TaskTemplates.deleteWhere {
                TaskTemplates.id.eq(id)
            }
        }
    }

    override suspend fun getTaskTemplates(): List<TaskTemplate> {
        return dbQuery {
            TaskTemplates.selectAll().mapNotNull { rowToTaskTemplate(it) }
        }
    }

    override suspend fun getTaskTemplates(itemTemplateId: Int): List<TaskTemplate> {
        return dbQuery {
            TaskTemplates.select {
                TaskTemplates.itemTemplateId.eq((itemTemplateId))
            }.mapNotNull { rowToTaskTemplate(it) }
        }
    }

    private fun rowToTodo(row: ResultRow?): Todo? {
        if (row == null) {
            return null
        }
        return Todo(
            id = row[Todos.id],
            userId = row[Todos.userId],
            todo = row[Todos.todo],
            done = row[Todos.done]
        )
    }

    private fun rowToUser(row: ResultRow?): User? {
        if (row == null) {
            return null
        }
        return User(
            userId = row[Users.userId],
            email = row[Users.email],
            displayName = row[Users.displayName],
            passwordHash = row[Users.passwordHash]
        )
    }

    private fun rowToItemTemplate(row: ResultRow?): ItemTemplate? {
        if (row == null) {
            return null
        }
        return ItemTemplate(
            id = row[ItemTemplates.id],
            title = row[ItemTemplates.title]
        )
    }

    private fun rowToTaskTemplate(row: ResultRow?): TaskTemplate? {
        if (row == null) {
            return null
        }
        return TaskTemplate(
            id = row[TaskTemplates.id],
            title = row[TaskTemplates.title],
            itemTemplateId = row[TaskTemplates.itemTemplateId],
            taskTemplateDependencyId = row[TaskTemplates.taskTemplateDependencyId],
            workerType = row[TaskTemplates.workerType],
            timeToComplete = row[TaskTemplates.timeToComplete],
            isAdditional = row[TaskTemplates.isAdditional]
        )
    }
}