package repository

import io.defolters.models.ItemTemplate
import io.defolters.models.TaskTemplate
import models.Todo
import models.User

interface Repository {

    // User
    suspend fun addUser(
        email: String,
        displayName: String,
        passwordHash: String
    ): User?

    suspend fun deleteUser(userId: Int)
    suspend fun findUser(userId: Int): User?
    suspend fun findUserByEmail(email: String): User?

    // Todos
    suspend fun addTodo(userId: Int, todo: String, done: Boolean): Todo?
    suspend fun deleteTodo(userId: Int, todoId: Int)
    suspend fun findTodo(userId: Int, todoId: Int): Todo?
    suspend fun getTodos(userId: Int): List<Todo>
    suspend fun getTodos(userId: Int, offset: Int, limit: Int = 100): List<Todo>

    // ItemTemplates
    suspend fun addItemTemplate(title: String): ItemTemplate?
    suspend fun deleteItemTemplate(id: Int)
    suspend fun getItemTemplates(): List<ItemTemplate>
    //update fun

    // TaskTemplates
    suspend fun addTaskTemplate(
        title: String,
        itemTemplateId: Int,
        taskTemplateDependencyId: Int?,
        workerType: String,
        timeToComplete: Int,
        isAdditional: Boolean
    ): TaskTemplate?

    suspend fun deleteTaskTemplate(id: Int)
    suspend fun getTaskTemplates(): List<TaskTemplate>
    suspend fun getTaskTemplates(itemTemplateId: Int): List<TaskTemplate>
    //update fun

    // Orders
}