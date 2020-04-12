package io.defolters.routes

import io.defolters.API_VERSION
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
import repository.Repository

const val ITEM_TEMPLATES = "$API_VERSION/item-templates"

@KtorExperimentalLocationsAPI
@Location(ITEM_TEMPLATES)
class ItemTemplatesRoute

@KtorExperimentalLocationsAPI
fun Route.itemTemplates(db: Repository) {
    authenticate("jwt") {
        post<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@post

            val itemTemplateParameters = call.receive<Parameters>()

            val title = itemTemplateParameters["title"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing title")


            try {
                db.addItemTemplate(title)?.let { itemTemplate ->
                    itemTemplate.id.let {
                        call.respond(HttpStatusCode.OK, itemTemplate)
                    }
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
