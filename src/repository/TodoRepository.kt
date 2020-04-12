

package repository

import io.defolters.repository.Users
import io.defolters.repository.Todos
import models.Todo
import models.User
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.statements.InsertStatement
import repository.DatabaseFactory.dbQuery

class TodoRepository: Repository {

    override suspend fun addUser(email: String, displayName: String, passwordHash: String) : User? {
        var statement : InsertStatement<Number>? = null
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

    override suspend fun findUserByEmail(email: String)= dbQuery {
        Users.select { Users.email.eq(email) }
            .map { rowToUser(it) }.singleOrNull()
    }

    override suspend fun addTodo(userId: Int, todo: String, done: Boolean): Todo? {
        var statement : InsertStatement<Number>? = null
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
            }.limit(limit, offset = offset).mapNotNull { rowToTodo(it) }
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
}