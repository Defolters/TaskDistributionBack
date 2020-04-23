package repository

import io.defolters.models.*
import io.defolters.repository.interfaces.*
import io.defolters.repository.tables.*
import io.defolters.routes.OrderJSON
import models.Todo
import models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import repository.DatabaseFactory.dbQuery
import java.util.logging.Level
import java.util.logging.Logger

class Repository : UserRepository, TodoRepository, ItemTemplateRepository, TaskTemplateRepository, OrderRepository,
    ItemRepository, TaskRepository, WorkerTypeRepository {

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
        workerTypeId: Int,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate? {
        //check dependency and then insert

        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = TaskTemplates.insert {
                it[TaskTemplates.title] = title
                it[TaskTemplates.itemTemplateId] = itemTemplateId
                it[TaskTemplates.taskTemplateDependencyId] = taskTemplateDependencyId
                it[TaskTemplates.workerTypeId] = workerTypeId
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

    override suspend fun findTaskTemplate(id: Int): TaskTemplate? {
        return dbQuery {
            TaskTemplates.select {
                TaskTemplates.id.eq(id)
            }.mapNotNull { it.rowToTaskTemplate() }.singleOrNull()
        }
    }

    override suspend fun addOrder(orderJSON: OrderJSON, time: Long): Order? {
        var orderInsertStatement: InsertStatement<Number>? = null
        dbQuery {
            val items = orderJSON.items
            val price = items.sumByDouble { it.price }

            // create order
            orderInsertStatement = Orders.insert {
                it[Orders.customerName] = orderJSON.customerName
                it[Orders.customerEmail] = orderJSON.customerEmail
                it[Orders.price] = price
                it[Orders.createdAt] = time
            }
            val order = orderInsertStatement?.resultedValues?.get(0)?.rowToOrder()!!

            // create items
            items.forEach { item ->
                // get item template
                val itemTemplate = ItemTemplates.select {
                    ItemTemplates.id.eq((item.itemTemplateId))
                }.mapNotNull { it.rowToItemTemplate() }.single()

                //create item
                val itemInsertStatement = Items.insert {
                    it[Items.orderId] = order.id
                    it[Items.title] = itemTemplate.title
                    it[Items.info] = item.info
                    it[Items.price] = item.price
                }
                val newItem = itemInsertStatement.resultedValues?.get(0)?.rowToItem()!!

                // get all tasks for current item template
                val taskTemplates = TaskTemplates.select {
                    TaskTemplates.itemTemplateId.eq((itemTemplate.id))
                }.mapNotNull { it.rowToTaskTemplate() }

                // divide to mandatory and additional tasks
                val mandatoryTaskTemplates = taskTemplates.filter { !it.isAdditional }.toMutableList()
                val additionalTaskTemplates = taskTemplates.filter { it.isAdditional }
                val logger = Logger.getLogger("APP")

                // create tasks for each item
                val setOfTasks = mutableSetOf<Int>()

                //set dependency to previous task
                var previousTaskId = 0
                //create mandatory tasks
                var i = 0
                while (mandatoryTaskTemplates.isNotEmpty()) {
                    val taskTemplate = mandatoryTaskTemplates[i % mandatoryTaskTemplates.size]
                    logger.log(Level.INFO, "task: $taskTemplate")
                    if ((taskTemplate.taskTemplateDependencyId == null) ||
                        (taskTemplate.taskTemplateDependencyId in setOfTasks)
                    ) {
                        // create task
                        val taskInsertStatement = Tasks.insert {
                            it[Tasks.itemId] = newItem.id
                            it[Tasks.taskDependencyId] = previousTaskId
                            it[Tasks.workerTypeId] = taskTemplate.workerTypeId
                            it[Tasks.title] = taskTemplate.title
                            it[Tasks.timeToComplete] = taskTemplate.timeToComplete
                            it[Tasks.status] = TaskStatus.NEW
                        }
                        val newTask = taskInsertStatement.resultedValues?.get(0)?.rowToTask()!!
                        previousTaskId = newTask.id

                        logger.log(Level.INFO, "task $newTask")
                        setOfTasks.add(taskTemplate.id)
                        mandatoryTaskTemplates.removeAt(i % mandatoryTaskTemplates.size)
                    }
                    i++
                }

                //create additional list
                val additionalNewList = mutableListOf<TaskTemplate>()
                val idsList = item.taskTemplatesIds.map { it.id }.toMutableList()
                idsList.sortBy { it }

                logger.log(Level.INFO, "size ${idsList.size}")
                for (j in 0 until idsList.size) {
                    // we should accept only existing tasks
                    val taskTemplate = additionalTaskTemplates.find { it.id == idsList[j] }
                        ?: throw Exception()

                    taskTemplate.taskTemplateDependencyId?.let { id ->
                        if (id !in setOfTasks) { // if wew didn't it yet
                            idsList.add(id)
                        }
                    }

                    //add to task's list
                    additionalNewList.add(taskTemplate)
                }

                //create additional task
                i = 0
                while (additionalNewList.isNotEmpty()) {
                    val taskTemplate = additionalNewList[i % additionalNewList.size]
                    logger.log(Level.INFO, "task: $taskTemplate")

                    if ((taskTemplate.taskTemplateDependencyId == null) ||
                        (taskTemplate.taskTemplateDependencyId in setOfTasks)
                    ) {
                        // create task
                        val taskInsertStatement = Tasks.insert {
                            it[Tasks.itemId] = newItem.id
                            it[Tasks.taskDependencyId] = previousTaskId
                            it[Tasks.title] = taskTemplate.title
                            it[Tasks.workerTypeId] = taskTemplate.workerTypeId
                            it[Tasks.timeToComplete] = taskTemplate.timeToComplete
                            it[Tasks.status] = TaskStatus.NEW
                        }
                        val newTask = taskInsertStatement.resultedValues?.get(0)?.rowToTask()!!
                        previousTaskId = newTask.id

                        logger.log(Level.INFO, "task $newTask")
                        setOfTasks.add(taskTemplate.id)
                        additionalNewList.removeAt(i % additionalNewList.size)
                    }
                    i++
                }

                logger.log(Level.INFO, "set size ${setOfTasks.size}")

            }

        }
        return orderInsertStatement?.resultedValues?.get(0)?.rowToOrder()!!
    }

    override suspend fun addOrder(
        customerName: String,
        customerEmail: String,
        price: Double,
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
            // delete order
            Orders.deleteWhere {
                Orders.id.eq(id)
            }

            // get items
            val items = Items.select {
                Items.orderId.eq((id))
            }.mapNotNull { it.rowToItemTemplate() }

            // delete tasks
            items.forEach { item ->
                Tasks.deleteWhere {
                    Tasks.itemId.eq(item.id)
                }
            }

            // delete items
            Items.deleteWhere {
                Items.orderId.eq(id)
            }
        }
    }

    override suspend fun getOrders(): List<Order> {
        return dbQuery {
            Orders.selectAll().mapNotNull { it.rowToOrder() }
        }
    }

    override suspend fun findOrder(id: Int): Order? {
        return dbQuery {
            Orders.select {
                Orders.id.eq(id)
            }.mapNotNull { it.rowToOrder() }.singleOrNull()
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

    override suspend fun getTasks(): List<Task> {
        return dbQuery {
            Tasks.selectAll().mapNotNull { it.rowToTask() }
        }
    }

    override suspend fun getTasks(itemId: Int): List<Task> {
        return dbQuery {
            Tasks.select {
                Tasks.itemId.eq((itemId))
            }.mapNotNull { it.rowToTask() }
        }
    }

    override suspend fun addWorkerType(title: String): WorkerType? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = WorkerTypes.insert {
                it[WorkerTypes.title] = title
            }
        }
        return statement?.resultedValues?.get(0)?.rowToWorkerType()
    }

    override suspend fun deleteWorkerType(id: Int) {
        dbQuery {
            WorkerTypes.deleteWhere {
                WorkerTypes.id.eq(id)
            }
        }
    }

    override suspend fun getWorkerTypes(): List<WorkerType> {
        return dbQuery {
            WorkerTypes.selectAll().mapNotNull { it.rowToWorkerType() }
        }
    }

    override suspend fun findWorkerType(workerTypeId: Int?): WorkerType? {
        if (workerTypeId == null) return null

        return dbQuery {
            WorkerTypes.select {
                WorkerTypes.id.eq(workerTypeId)
            }.mapNotNull { it.rowToWorkerType() }.singleOrNull()
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
    workerTypeId = this[TaskTemplates.workerTypeId],
    timeToComplete = this[TaskTemplates.timeToComplete],
    isAdditional = this[TaskTemplates.isAdditional]
)

fun ResultRow.rowToOrder() = Order(
    id = this[Orders.id],
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

fun ResultRow.rowToTask() = Task(
    id = this[Tasks.id],
    itemId = this[Tasks.itemId],
    taskDependencyId = this[Tasks.taskDependencyId],
    workerTypeId = this[Tasks.workerTypeId],
    title = this[Tasks.title],
    timeToComplete = this[Tasks.timeToComplete],
    status = this[Tasks.status]
)

fun ResultRow.rowToWorkerType() = WorkerType(
    id = this[WorkerTypes.id],
    title = this[WorkerTypes.title]
)

