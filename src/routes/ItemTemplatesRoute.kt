package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.auth.MySession
import io.ktor.application.ApplicationCall
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.sessions.get
import io.ktor.sessions.sessions
import models.User
import repository.Repository

const val ITEM_TEMPLATES = "$API_VERSION/item-templates"

@KtorExperimentalLocationsAPI
@Location(ITEM_TEMPLATES)
class ItemTemplatesRoute

@KtorExperimentalLocationsAPI
fun Route.itemTemplates(db: Repository) {
    authenticate("jwt") {
        post<ItemTemplatesRoute> {
            val user = call.getActiveUser(db) ?: return@post

            val itemTemplateParameters = call.receive<Parameters>()

            val title = itemTemplateParameters["title"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing title")

            try {
                val currentTodo = db.addItemTemplate(title)
                currentTodo?.id?.let {
                    call.respond(HttpStatusCode.OK, currentTodo)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to add ItemTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems adding ItemTemplate")
            }
        }
        get<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@get

            try {
                val itemTemplates = db.getItemTemplates()
                call.respond(itemTemplates)
            } catch (e: Throwable) {
                application.log.error("Failed to get ItemTemplates", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting ItemTemplates")
            }
        }
        delete<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@delete

            val itemTemplateParameters = call.receive<Parameters>()
            // should i check or it will fall in the second check?
//            if (!itemTemplateParameters.contains("id")) {
//                return@delete call.respond(HttpStatusCode.BadRequest, "Missing itemTemplate Id")
//            }
            val itemTemplateId = itemTemplateParameters["id"]
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Missing itemTemplate Id"
                )

            try {
                db.deleteItemTemplate(itemTemplateId.toInt())
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete itemTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting itemTemplate")
            }
        }
    }
}

suspend fun ApplicationCall.isSessionActive(db: Repository): Boolean {
    val user = this.sessions.get<MySession>()?.let { db.findUser(it.userId) }
    if (user == null) {
        this.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
        return false
    }
    return true
}

suspend fun ApplicationCall.getActiveUser(db: Repository): User? {
    val user = this.sessions.get<MySession>()?.let { db.findUser(it.userId) }
    if (user == null) {
        this.respond(HttpStatusCode.BadRequest, "Problems retrieving User")
    }
    return user
}
