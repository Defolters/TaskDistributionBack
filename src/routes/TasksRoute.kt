package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.TaskRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.KtorExperimentalLocationsAPI
import io.ktor.locations.Location
import io.ktor.locations.get
import io.ktor.response.respond
import io.ktor.routing.Route

const val TASKS = "$API_VERSION/tasks"

@KtorExperimentalLocationsAPI
@Location(TASKS)
class TasksRoute

@KtorExperimentalLocationsAPI
fun Route.tasksRoute(db: TaskRepository) {
    authenticate("jwt") {
        get<TasksRoute> {
            call.getActiveUser(db) ?: return@get

            val tasksParameters = call.request.queryParameters
            val idemId = tasksParameters["idemId"]

            try {
                val tasks = if (idemId != null) {
                    db.getTasks(idemId.toInt())
                } else {
                    db.getTasks()
                }
                call.respond(tasks)
            } catch (e: Throwable) {
                application.log.error("Failed to get Task", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Task")
            }
        }
    }
}
