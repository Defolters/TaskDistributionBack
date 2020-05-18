package repository

import io.defolters.models.*
import io.defolters.optimization.ItemData
import io.defolters.optimization.TaskData
import io.defolters.optimization.TaskOptimizer
import io.defolters.repository.interfaces.*
import io.defolters.repository.tables.*
import io.defolters.routes.OrderJSON
import io.defolters.routes.ScheduleData
import io.defolters.routes.ScheduleTaskData
import io.defolters.routes.WorkerTypeData
import models.User
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.statements.InsertStatement
import repository.DatabaseFactory.dbQuery
import java.awt.Color
import java.util.logging.Level
import java.util.logging.Logger

class Repository : UserRepository, ItemTemplateRepository, TaskTemplateRepository, OrderRepository,
    ItemRepository, TaskRepository, WorkerTypeRepository, ScheduleRepository {

    override suspend fun optimize() {
        dbQuery {
            val itemsData = mutableListOf<ItemData>()
            val items = Items.select {
                Items.isReady eq false
            }.mapNotNull { it.rowToItem() }

            if (items.isEmpty()) return@dbQuery

            items.forEach { item ->
                val tasksData = mutableListOf<TaskData>()

                val tasks = Tasks.select {
                    Tasks.itemId eq item.id and (Tasks.status neq TaskStatus.DONE)
                }.mapNotNull { it.rowToTask() }

                tasks.forEach { task ->
                    tasksData.add(
                        TaskData(
                            task_id = task.id,
                            item_id = task.itemId,
                            workerType = task.workerTypeId,
                            timeToComplete = task.timeToComplete,
                            taskDependency = task.taskDependencyId,
                            isAdditional = task.isAdditional,
                            title = task.title,
                            color = item.color,
                            status = task.status
                        )
                    )
                }

                itemsData.add(ItemData(tasksData))
            }

            // OPTIMIZE
            // TODO: BEFORE CALLING WE SHOULD CHANGE TIME OF TASKS IN WORK
            val tasks = TaskOptimizer.optimizeFourth(itemsData)

            ScheduleTasks.deleteAll()
            tasks.forEach { taskData ->
                ScheduleTasks.insert {
                    it[ScheduleTasks.workerTypeId] = taskData.resourceId
                    it[ScheduleTasks.taskId] = taskData.taskId
                    it[ScheduleTasks.title] = taskData.title
                    it[ScheduleTasks.start] = taskData.start
                    it[ScheduleTasks.end] = taskData.end
                    it[ScheduleTasks.color] = taskData.bgColor
                }
            }
        }
    }

    override suspend fun addUser(
        email: String,
        displayName: String,
        passwordHash: String,
        userType: UserType,
        workerTypeId: Int?
    ): User? {
        var statement: InsertStatement<Number>? = null
        dbQuery {
            statement = Users.insert {
                it[Users.email] = email
                it[Users.displayName] = displayName
                it[Users.passwordHash] = passwordHash
                it[Users.userType] = userType
                it[Users.workerTypeId] = workerTypeId
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
            ItemTemplates.selectAll().mapNotNull { it.rowToItemTemplate() }.sortedBy { it.id }
        }
    }

    override suspend fun findItemTemplate(id: Int?): ItemTemplate? {
        if (id == null) return null

        return dbQuery {
            ItemTemplates.select {
                ItemTemplates.id.eq(id)
            }.mapNotNull { it.rowToItemTemplate() }.singleOrNull()
        }
    }

    override suspend fun updateItemTemplate(id: Int, title: String): ItemTemplate? {
        return dbQuery {
            ItemTemplates.update({ ItemTemplates.id eq id }) {
                it[ItemTemplates.title] = title
            }
            ItemTemplates.select {
                ItemTemplates.id.eq(id)
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
            TaskTemplates.selectAll().mapNotNull { it.rowToTaskTemplate() }.sortedBy { it.id }
        }
    }

    override suspend fun getTaskTemplates(itemTemplateId: Int): List<TaskTemplate> {
        return dbQuery {
            TaskTemplates.select {
                TaskTemplates.itemTemplateId.eq((itemTemplateId))
            }.mapNotNull { it.rowToTaskTemplate() }
        }
    }

    override suspend fun getTaskTemplates(itemTemplateId: Int, isAdditional: Boolean): List<TaskTemplate> {
        return dbQuery {
            TaskTemplates.select {
                TaskTemplates.itemTemplateId.eq((itemTemplateId)) and
                        TaskTemplates.isAdditional.eq((isAdditional))
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

    override suspend fun updateTaskTemplate(
        id: Int,
        title: String,
        itemTemplateId: Int,
        taskTemplateDependencyId: Int?,
        workerTypeId: Int,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate? {
        return dbQuery {
            TaskTemplates.update({ TaskTemplates.id eq id }) {
                it[TaskTemplates.title] = title
                it[TaskTemplates.itemTemplateId] = itemTemplateId
                it[TaskTemplates.taskTemplateDependencyId] = taskTemplateDependencyId
                it[TaskTemplates.workerTypeId] = workerTypeId
                it[TaskTemplates.timeToComplete] = timeToComplete
                it[TaskTemplates.isAdditional] = isAdditional
            }
            TaskTemplates.select {
                TaskTemplates.id.eq(id)
            }.mapNotNull { it.rowToTaskTemplate() }.singleOrNull()
        }
    }

    override suspend fun addOrder(orderJSON: OrderJSON, time: String): Order? {
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
                it[Orders.isReady] = false
            }
            val order = orderInsertStatement?.resultedValues?.get(0)?.rowToOrder()!!

            // create items
            items.forEach { item ->
                // get item template
                val itemTemplate = ItemTemplates.select {
                    ItemTemplates.id.eq((item.itemTemplateId))
                }.mapNotNull { it.rowToItemTemplate() }.single()

                //create item
                val color = generateRandomColor()
                val itemInsertStatement = Items.insert {
                    it[Items.orderId] = order.id
                    it[Items.title] = itemTemplate.title
                    it[Items.info] = item.info
                    it[Items.price] = item.price
                    it[Items.isReady] = false
                    it[Items.color] = color
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
                val map = mutableMapOf<Int, Int>()

                //create mandatory tasks
                var i = 0
                while (mandatoryTaskTemplates.isNotEmpty()) {
                    val taskTemplate = mandatoryTaskTemplates[i % mandatoryTaskTemplates.size]
                    logger.log(Level.INFO, "task: $taskTemplate")
                    if ((taskTemplate.taskTemplateDependencyId == null) ||
                        (taskTemplate.taskTemplateDependencyId in map)
                    ) {
                        // create task
                        val taskInsertStatement = Tasks.insert {
                            it[Tasks.itemId] = newItem.id
                            it[Tasks.taskDependencyId] = map[taskTemplate.taskTemplateDependencyId]
                            it[Tasks.workerTypeId] = taskTemplate.workerTypeId
                            it[Tasks.title] = taskTemplate.title
                            it[Tasks.timeToComplete] = taskTemplate.timeToComplete
                            it[Tasks.isAdditional] = taskTemplate.isAdditional
                            it[Tasks.status] = TaskStatus.NEW
                            it[Tasks.lastStatusUpdate] = time
                        }
                        val newTask = taskInsertStatement.resultedValues?.get(0)?.rowToTask()!!

                        logger.log(Level.INFO, "task $newTask")
                        map[taskTemplate.id] = newTask.id
                        mandatoryTaskTemplates.removeAt(i % mandatoryTaskTemplates.size)
                    }
                    i++
                }

                //create additional list
                val additionalNewList = mutableListOf<TaskTemplate>()
                val idsList = item.taskTemplatesIds?.map { it }?.toMutableList() ?: mutableListOf() //.id
                idsList.sortBy { it }

                logger.log(Level.INFO, "size ${idsList.size}")
                i = 0
                while (idsList.isNotEmpty()) {
                    val taskId = idsList[i % idsList.size]

                    // we should accept only existing tasks
                    val taskTemplate = additionalTaskTemplates.find { it.id == taskId }
                        ?: throw Exception()

                    taskTemplate.taskTemplateDependencyId?.let { id ->
                        if ((id !in map) && (!additionalNewList.any { it.id == id }) && (id !in idsList)) {
                            idsList.add(id)
                        }
                    }

                    //add to task's list
                    additionalNewList.add(taskTemplate)
                    idsList.removeAt(i % idsList.size)
                    i++
                }

                //create additional task
                i = 0
                while (additionalNewList.isNotEmpty()) {
                    val taskTemplate = additionalNewList[i % additionalNewList.size]
                    logger.log(Level.INFO, "task: $taskTemplate")

                    if ((taskTemplate.taskTemplateDependencyId == null) ||
                        (taskTemplate.taskTemplateDependencyId in map)
                    ) {
                        // create task
                        val taskInsertStatement = Tasks.insert {
                            it[Tasks.itemId] = newItem.id
                            it[Tasks.taskDependencyId] = map[taskTemplate.taskTemplateDependencyId]
                            it[Tasks.title] = taskTemplate.title
                            it[Tasks.workerTypeId] = taskTemplate.workerTypeId
                            it[Tasks.timeToComplete] = taskTemplate.timeToComplete
                            it[Tasks.isAdditional] = taskTemplate.isAdditional
                            it[Tasks.status] = TaskStatus.NEW
                            it[Tasks.lastStatusUpdate] = time
                        }
                        val newTask = taskInsertStatement.resultedValues?.get(0)?.rowToTask()!!

                        logger.log(Level.INFO, "task $newTask")
                        map[taskTemplate.id] = newTask.id
                        additionalNewList.removeAt(i % additionalNewList.size)
                    }
                    i++
                }

                logger.log(Level.INFO, "map size ${map.size}")

            }

        }
        return orderInsertStatement?.resultedValues?.get(0)?.rowToOrder()!!
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
            Orders.selectAll().mapNotNull { it.rowToOrder() }.sortedBy { it.id }
        }
    }

    override suspend fun findOrder(id: Int): Order? {
        return dbQuery {
            Orders.select {
                Orders.id.eq(id)
            }.mapNotNull { it.rowToOrder() }.singleOrNull()
        }
    }

    override suspend fun updateOrder(id: Int, customerName: String, customerEmail: String): Order? {
        return dbQuery {
            Orders.update({ Orders.id eq id }) {
                it[Orders.customerName] = customerName
                it[Orders.customerEmail] = customerEmail
            }
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
            Items.selectAll().mapNotNull { it.rowToItem() }.sortedBy { it.id }
        }
    }

    override suspend fun getItems(orderId: Int): List<Item> {
        return dbQuery {
            Items.select {
                Items.orderId.eq((orderId))
            }.mapNotNull { it.rowToItem() }
        }
    }

    override suspend fun findItem(id: Int?): Item? {
        if (id == null) return null

        return dbQuery {
            Items.select {
                Items.id.eq(id)
            }.mapNotNull { it.rowToItem() }.singleOrNull()
        }
    }

    override suspend fun getTasks(): List<Task> {
        return dbQuery {
            Tasks.selectAll().mapNotNull { it.rowToTask() }.sortedBy { it.id }
        }
    }

    override suspend fun getItemTasks(itemId: Int): List<Task> {
        return dbQuery {
            Tasks.select {
                Tasks.itemId.eq((itemId))
            }.mapNotNull { it.rowToTask() }
        }
    }

    override suspend fun getWorkerTasks(workerTypeId: Int): List<Task> {
        return dbQuery {
            Tasks.select {
                Tasks.workerTypeId.eq((workerTypeId))
            }.mapNotNull { it.rowToTask() }
        }
    }

    override suspend fun findTask(id: Int?): Task? {
        if (id == null) return null

        return dbQuery {
            Tasks.select {
                Tasks.id.eq(id)
            }.mapNotNull { it.rowToTask() }.singleOrNull()
        }
    }

    override suspend fun updateTaskStatus(id: Int, status: TaskStatus): Task? {
        return dbQuery {
            Tasks.update({ Tasks.id eq id }) {
                it[Tasks.status] = status
            }
            Tasks.select {
                Tasks.id.eq(id)
            }.mapNotNull { it.rowToTask() }.singleOrNull()
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
            WorkerTypes.selectAll().mapNotNull { it.rowToWorkerType() }.sortedBy { it.id }
        }
    }

    override suspend fun findWorkerType(id: Int?): WorkerType? {
        if (id == null) return null

        return dbQuery {
            WorkerTypes.select {
                WorkerTypes.id.eq(id)
            }.mapNotNull { it.rowToWorkerType() }.singleOrNull()
        }
    }

    override suspend fun updateWorkerType(id: Int, title: String): WorkerType? {
        return dbQuery {
            WorkerTypes.update({ WorkerTypes.id eq id }) {
                it[WorkerTypes.title] = title
            }
            WorkerTypes.select {
                WorkerTypes.id.eq(id)
            }.mapNotNull { it.rowToWorkerType() }.singleOrNull()
        }
    }

    override suspend fun getSchedule(): ScheduleData? {
        return dbQuery {
            val workerTypesData = mutableListOf<WorkerTypeData>()

            val workerTypes = WorkerTypes.selectAll().mapNotNull { it.rowToWorkerType() }.sortedBy { it.id }
            workerTypes.forEach { workerType ->
                workerTypesData.add(WorkerTypeData(workerType.id, workerType.title))
            }

            val tasks = ScheduleTasks.selectAll().mapNotNull { it.rowToScheduleTask() }

            ScheduleData(workerTypesData, tasks)
        }
    }
}

fun ResultRow.rowToUser() = User(
    userId = this[Users.userId],
    email = this[Users.email],
    displayName = this[Users.displayName],
    passwordHash = this[Users.passwordHash],
    userType = this[Users.userType],
    workerTypeId = this[Users.workerTypeId]
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
    createdAt = this[Orders.createdAt],
    isReady = this[Orders.isReady]
)

fun ResultRow.rowToItem() = Item(
    id = this[Items.id],
    orderId = this[Items.orderId],
    price = this[Items.price],
    title = this[Items.title],
    info = this[Items.info],
    isReady = this[Items.isReady],
    color = this[Items.color]
)

fun ResultRow.rowToTask() = Task(
    id = this[Tasks.id],
    itemId = this[Tasks.itemId],
    taskDependencyId = this[Tasks.taskDependencyId],
    workerTypeId = this[Tasks.workerTypeId],
    title = this[Tasks.title],
    timeToComplete = this[Tasks.timeToComplete],
    isAdditional = this[Tasks.isAdditional],
    status = this[Tasks.status],
    lastStatusUpdate = this[Tasks.lastStatusUpdate]
)

fun ResultRow.rowToWorkerType() = WorkerType(
    id = this[WorkerTypes.id],
    title = this[WorkerTypes.title]
)

fun ResultRow.rowToScheduleTask() = ScheduleTaskData(
    id = this[ScheduleTasks.id],
    resourceId = this[ScheduleTasks.workerTypeId],
    taskId = this[ScheduleTasks.taskId],
    start = this[ScheduleTasks.start],
    end = this[ScheduleTasks.end],
    title = this[ScheduleTasks.title],
    bgColor = this[ScheduleTasks.color]
)

fun generateRandomColor(mix: Color? = null): String {
    var red: Int = kotlin.random.Random.nextInt(250)
    var green: Int = kotlin.random.Random.nextInt(250)
    var blue: Int = kotlin.random.Random.nextInt(250)

    // mix the color
    if (mix != null) {
        red = (red + mix.red) / 2
        green = (green + mix.green) / 2
        blue = (blue + mix.blue) / 2
    }
    val hex = java.lang.String.format("#%02x%02x%02x", red, green, blue)
    print("color ${hex}")
    return hex
}

