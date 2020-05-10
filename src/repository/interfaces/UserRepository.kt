package io.defolters.repository.interfaces

import io.defolters.models.UserType
import models.User

interface UserRepository : RepositoryInterface {
    suspend fun addUser(
        email: String,
        displayName: String,
        passwordHash: String,
        userType: UserType,
        workerTypeId: Int?
    ): User?

    suspend fun deleteUser(userId: Int)
    suspend fun findUser(userId: Int): User?
    suspend fun findUserByEmail(email: String): User?
}