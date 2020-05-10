package models

import io.defolters.models.UserType
import io.ktor.auth.Principal
import java.io.Serializable

data class User(
    val userId: Int,
    val email: String,
    val displayName: String,
    val passwordHash: String,
    val userType: UserType,
    val workerTypeId: Int?
) : Serializable, Principal

