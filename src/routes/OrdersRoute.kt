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
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

const val ORDERS = "$API_VERSION/orders"

@KtorExperimentalLocationsAPI
@Location(ORDERS)
class OrdersRoute

data class OrderJSON(val hello: String, val items: List<ItemJSON>)
data class ItemJSON(val itemTemplateId: Int, val price: Float, val taskTemplatesIds: List<Int>)

@KtorExperimentalLocationsAPI
fun Route.orders(db: Repository) {
    authenticate("jwt") {
        post<OrdersRoute> {
            call.getActiveUser(db) ?: return@post

            // TODO: Add creating of orders
            val date = Calendar.getInstance().time
            val logger = Logger.getLogger("APP")
            logger.log(Level.INFO, "date: $date")

            val orderJSON = call.receive<OrderJSON>()
            call.respond(HttpStatusCode.OK, orderJSON)
        }
        get<OrdersRoute> {
            call.getActiveUser(db) ?: return@get

            try {
                val orders = db.getOrders()
                call.respond(orders)
            } catch (e: Throwable) {
                application.log.error("Failed to get Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Order")
            }
        }
        delete<OrdersRoute> {
            call.getActiveUser(db) ?: return@delete

            val ordersParameters = call.receive<Parameters>()
            val orderId = ordersParameters["id"]
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Missing orderId"
                )

            try {
                db.deleteOrder(orderId.toInt())
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting Order")
            }
        }
    }
}
