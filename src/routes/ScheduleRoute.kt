package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.ScheduleRepository
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

const val SCHEDULE = "$API_VERSION/schedule"

@KtorExperimentalLocationsAPI
@Location(SCHEDULE)
class ScheduleRoute

data class WorkerTypeData(val id: Int, val name: String)
data class ScheduleTaskData(
    val id: Int,
    val resourceId: Int,
    val start: String,
    val end: String,
    val title: String,
    val bgColor: String = "red"
)

data class ScheduleData(val workerTypes: List<WorkerTypeData>, val tasks: List<ScheduleTaskData>)

@KtorExperimentalLocationsAPI
fun Route.scheduleRoute(db: ScheduleRepository) {
    authenticate("jwt") {
        get<ScheduleRoute> {
            call.getActiveUser(db) ?: return@get

            try {
                val schedule = db.getSchedule()
                if (schedule != null) {
                    call.respond(schedule)
                } else {
                    throw Exception("empty")
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get schedule", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting schedule")
            }
        }
    }
}
