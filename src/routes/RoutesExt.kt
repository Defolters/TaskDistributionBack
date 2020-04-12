package io.defolters.routes

import io.defolters.auth.MySession
import io.ktor.application.ApplicationCall
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import models.User
import repository.Repository

suspend fun ApplicationCall.getActiveUser(db: Repository): User? {
    val user = this.sessions.get<MySession>()?.let { db.findUser(it.userId) }
    if (user == null) {
        this.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
    }
    return user
}