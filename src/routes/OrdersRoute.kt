package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.OrderRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.system.measureTimeMillis

const val ORDERS = "$API_VERSION/orders"

@KtorExperimentalLocationsAPI
@Location(ORDERS)
class OrdersRoute

@KtorExperimentalLocationsAPI
@Location("$ORDERS/{id}")
data class OrdersIdRoute(val id: Int)

data class OrderJSON(val customerName: String, val customerEmail: String, val items: List<ItemJSON>, val id: List<Int>?)
data class ItemJSON(val itemTemplateId: Int, val info: String, val price: Double, val taskTemplatesIds: List<TaskJSON>)
data class TaskJSON(val id: Int)

@KtorExperimentalLocationsAPI
fun Route.ordersRoute(db: OrderRepository) {
    authenticate("jwt") {
        post<OrdersRoute> {
            call.getActiveUser(db) ?: return@post

            // TODO: Add creating of orders
            val date = Calendar.getInstance().time
            val logger = Logger.getLogger("APP")
            logger.log(Level.INFO, "date: $date")

            try {
                val orderJSON = call.receive<OrderJSON>()
                val time = measureTimeMillis {
                    db.addOrder(orderJSON, date.time)?.let { order ->
                        call.respond(order)
                    }
                }
                logger.log(Level.INFO, "time to create order: $time")

            } catch (e: Throwable) {
                application.log.error("Failed to get Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Order")
            }
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
        get<OrdersIdRoute> { params ->
            call.getActiveUser(db) ?: return@get

            try {
                db.findOrder(params.id)?.let { order ->
                    call.respond(order)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Order")
            }
        }
        delete<OrdersRoute> {
            call.getActiveUser(db) ?: return@delete

            val jsonData = call.receive<WorkerTypeJSON>()

            val ids = jsonData.id
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ids")

            try {
                ids.forEach { id ->
                    db.deleteOrder(id)
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting Order")
            }
        }
    }
}
