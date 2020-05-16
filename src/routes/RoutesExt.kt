package io.defolters.routes

import io.defolters.auth.MySession
import io.defolters.repository.interfaces.RepositoryInterface
import io.defolters.repository.interfaces.UserRepository
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import models.User

// TODO: ANDROID APP SESSIONS DOESN'T WORK
suspend fun ApplicationCall.getActiveUser(db: RepositoryInterface): User? {
    val user = this.sessions.get<MySession>()?.let { (db as? UserRepository)?.findUser(it.userId) }
    if (user == null) {
        this.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
    }
    return user
}