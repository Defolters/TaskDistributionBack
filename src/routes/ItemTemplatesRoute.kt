package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.ItemTemplateRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

const val ITEM_TEMPLATES = "$API_VERSION/item-templates"

@KtorExperimentalLocationsAPI
@Location(ITEM_TEMPLATES)
class ItemTemplatesRoute

@KtorExperimentalLocationsAPI
@Location("$ITEM_TEMPLATES/{id}")
data class ItemTemplatesIdRoute(val id: Int)

data class ItemTemplateJSON(val id: Int, val title: String, val ids: List<Int>?)

@KtorExperimentalLocationsAPI
fun Route.itemTemplatesRoute(db: ItemTemplateRepository) {
    authenticate("jwt") {
        post<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@post

            val jsonData = call.receive<ItemTemplateJSON>()

            val title = jsonData.title

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
        get<ItemTemplatesIdRoute> { idRoute ->
            call.getActiveUser(db) ?: return@get

            try {
                db.findItemTemplate(idRoute.id)?.let { itemTemplate ->
                    call.respond(itemTemplate)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get ItemTemplates", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting ItemTemplates")
            }
        }
        delete<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@delete

            val jsonData = call.receive<ItemTemplateJSON>()

            val ids = jsonData.ids
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ids")

            try {
                ids.forEach { id ->
                    db.deleteItemTemplate(id)
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete ItemTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting ItemTemplate")
            }
        }
        put<ItemTemplatesRoute> {
            call.getActiveUser(db) ?: return@put

            val jsonData = call.receive<ItemTemplateJSON>()

            try {
                db.updateItemTemplate(jsonData.id, jsonData.title)?.let {
                    call.respond(it)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to update ItemTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems updating ItemTemplate")
            }
        }
    }
}
