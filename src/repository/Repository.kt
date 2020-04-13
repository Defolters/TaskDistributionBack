package repository

import io.defolters.models.Item
import io.defolters.models.ItemTemplate
import io.defolters.models.Order
import io.defolters.models.TaskTemplate
import io.defolters.repository.interfaces.*
import io.defolters.repository.tables.*
import models.Todo
import models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import repository.DatabaseFactory.dbQuery

class Repository : UserRepository, TodoRepository, ItemTemplateRepository, TaskTemplateRepository, OrderRepository,
    ItemRepository, TaskRepository {

    override suspend fun addUser(email: String, displayName: String, passwordHash: String): User? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Users.insert {
                it[Users.email] = email
                it[Users.displayName] = displayName
                it[Users.passwordHash] = passwordHash
            }
        }
        return statement?.resultedValues?.get(0)?.rowToUser()
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
            .map { it.rowToUser() }.singleOrNull()
    }

    override suspend fun findUserByEmail(email: String) = dbQuery {
        Users.select { Users.email.eq(email) }
            .map { it.rowToUser() }.singleOrNull()
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
        return statement?.resultedValues?.get(0)?.rowToTodo()
    }

    override suspend fun getTodos(userId: Int): List<Todo> {
        return dbQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.mapNotNull { it.rowToTodo() }
        }
    }

    override suspend fun getTodos(userId: Int, offset: Int, limit: Int): List<Todo> {
        return dbQuery {
            Todos.select {
                Todos.userId.eq((userId))
            }.limit(limit, offset = offset.toLong()).mapNotNull { it.rowToTodo() }
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
            }.map { it.rowToTodo() }.singleOrNull()
        }
    }

    override suspend fun addItemTemplate(title: String): ItemTemplate? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = ItemTemplates.insert {
                it[ItemTemplates.title] = title
            }
        }
        return statement?.resultedValues?.get(0)?.rowToItemTemplate()
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
            ItemTemplates.selectAll().mapNotNull { it.rowToItemTemplate() }
        }
    }

    override suspend fun findItemTemplate(itemTemplateId: Int?): ItemTemplate? {
        if (itemTemplateId == null) return null

        return dbQuery {
            ItemTemplates.select {
                ItemTemplates.id.eq(itemTemplateId)
            }.mapNotNull { it.rowToItemTemplate() }.singleOrNull()
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
        return statement?.resultedValues?.get(0)?.rowToTaskTemplate()
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
            TaskTemplates.selectAll().mapNotNull { it.rowToTaskTemplate() }
        }
    }

    override suspend fun getTaskTemplates(itemTemplateId: Int): List<TaskTemplate> {
        return dbQuery {
            TaskTemplates.select {
                TaskTemplates.itemTemplateId.eq((itemTemplateId))
            }.mapNotNull { it.rowToTaskTemplate() }
        }
    }

    override suspend fun addOrder(
        customerName: String,
        customerEmail: String,
        price: Float,
        createdAt: Long
    ): Order? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Orders.insert {
                it[Orders.customerName] = customerName
                it[Orders.customerEmail] = customerEmail
                it[Orders.price] = price
                it[Orders.createdAt] = createdAt
            }
        }
        return statement?.resultedValues?.get(0)?.rowToOrder()
    }

    override suspend fun deleteOrder(id: Int) {
        dbQuery {
            Orders.deleteWhere {
                Orders.id.eq(id)
            }
        }
    }

    override suspend fun getOrders(): List<Order> {
        return dbQuery {
            Orders.selectAll().mapNotNull { it.rowToOrder() }
        }
    }

    override suspend fun deleteItem(id: Int) {
        dbQuery {
            Items.deleteWhere {
                Items.id.eq(id)
            }
        }
    }

    override suspend fun getItems(): List<Item> {
        return dbQuery {
            Items.selectAll().mapNotNull { it.rowToItem() }
        }
    }

    override suspend fun getItems(orderId: Int): List<Item> {
        return dbQuery {
            Items.select {
                Items.orderId.eq((orderId))
            }.mapNotNull { it.rowToItem() }
        }
    }
}

fun ResultRow.rowToTodo() = Todo(
    id = this[Todos.id],
    userId = this[Todos.userId],
    todo = this[Todos.todo],
    done = this[Todos.done]
)

fun ResultRow.rowToUser() = User(
    userId = this[Users.userId],
    email = this[Users.email],
    displayName = this[Users.displayName],
    passwordHash = this[Users.passwordHash]
)

fun ResultRow.rowToItemTemplate() = ItemTemplate(
    id = this[ItemTemplates.id],
    title = this[ItemTemplates.title]
)

fun ResultRow.rowToTaskTemplate() = TaskTemplate(
    id = this[TaskTemplates.id],
    title = this[TaskTemplates.title],
    itemTemplateId = this[TaskTemplates.itemTemplateId],
    taskTemplateDependencyId = this[TaskTemplates.taskTemplateDependencyId],
    workerType = this[TaskTemplates.workerType],
    timeToComplete = this[TaskTemplates.timeToComplete],
    isAdditional = this[TaskTemplates.isAdditional]
)

fun ResultRow.rowToOrder() = Order(
    id = this[ItemTemplates.id],
    customerName = this[Orders.customerName],
    customerEmail = this[Orders.customerEmail],
    price = this[Orders.price],
    createdAt = this[Orders.createdAt]
)

fun ResultRow.rowToItem() = Item(
    id = this[Items.id],
    orderId = this[Items.orderId],
    price = this[Items.price],
    title = this[Items.title],
    info = this[Items.info]
)
