package io.defolters.repository.interfaces

import models.Todo

interface TodoRepository : RepositoryInterface {
    suspend fun addTodo(userId: Int, todo: String, done: Boolean): Todo?
    suspend fun deleteTodo(userId: Int, todoId: Int)
    suspend fun findTodo(userId: Int, todoId: Int): Todo?
    suspend fun getTodos(userId: Int): List<Todo>
    suspend fun getTodos(userId: Int, offset: Int, limit: Int = 100): List<Todo>
}