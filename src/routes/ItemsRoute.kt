package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.ItemRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.delete
import io.ktor.locations.get
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

const val ITEMS = "$API_VERSION/items"

@KtorExperimentalLocationsAPI
@Location(ITEMS)
class ItemsRoute

@KtorExperimentalLocationsAPI
fun Route.itemsRoute(db: ItemRepository) {
    authenticate("jwt") {
        get<ItemsRoute> {
            call.getActiveUser(db) ?: return@get

            val itemsParameters = call.request.queryParameters
            val orderId = itemsParameters["orderId"]

            try {
                val taskTemplates = if (orderId != null) {
                    db.getItems(orderId.toInt())
                } else {
                    db.getItems()
                }
                call.respond(taskTemplates)
            } catch (e: Throwable) {
                application.log.error("Failed to get Item", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Item")
            }
        }
        delete<ItemsRoute> {
            call.getActiveUser(db) ?: return@delete

            val itemParameters = call.receive<Parameters>()
            val itemId = itemParameters["id"]
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Missing itemId"
                )

            try {
                db.deleteItem(itemId.toInt())
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete Item", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting Item")
            }
        }
    }
}
