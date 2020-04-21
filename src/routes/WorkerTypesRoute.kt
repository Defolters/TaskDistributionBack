package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.WorkerTypeRepository
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

const val WORKER_TYPES = "$API_VERSION/worker-types"

@KtorExperimentalLocationsAPI
@Location(WORKER_TYPES)
class WorkerTypesRoute

@KtorExperimentalLocationsAPI
@Location("$WORKER_TYPES/{id}")
data class WorkerTypesIdRoute(val id: Int)

@KtorExperimentalLocationsAPI
fun Route.workerTypesRoute(db: WorkerTypeRepository) {
    authenticate("jwt") {
        post<WorkerTypesRoute> {
            call.getActiveUser(db) ?: return@post

            val workerTypeParameters = call.receive<Parameters>()

            val title = workerTypeParameters["title"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing title")


            try {
                db.addWorkerType(title)?.let { workerType ->
                    workerType.id.let {
                        call.respond(HttpStatusCode.OK, workerType)
                    }
                }
            } catch (e: Throwable) {
                application.log.error("Failed to add WorkerType", e)
                call.respond(HttpStatusCode.BadRequest, "Problems adding WorkerType")
            }
        }
        get<WorkerTypesRoute> {
            call.getActiveUser(db) ?: return@get

            try {
                val workerTypes = db.getWorkerTypes()
                call.respond(workerTypes)
            } catch (e: Throwable) {
                application.log.error("Failed to get WorkerType", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting WorkerType")
            }
        }
        get<WorkerTypesIdRoute> { workerTypesIdRoute ->
            call.getActiveUser(db) ?: return@get

            try {
                db.findWorkerType(workerTypesIdRoute.id)?.let { workerType ->
                    call.respond(workerType)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get WorkerType", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting WorkerType")
            }
        }
        delete<WorkerTypesIdRoute> { workerTypesIdRoute ->
            call.getActiveUser(db) ?: return@delete

            try {
                db.deleteWorkerType(workerTypesIdRoute.id)
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete WorkerType", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting WorkerType")
            }
        }
    }
}
