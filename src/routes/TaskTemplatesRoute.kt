package io.defolters.routes

import com.google.gson.Gson
import io.defolters.API_VERSION
import io.defolters.repository.interfaces.TaskTemplateRepository
import io.ktor.application.application
import io.ktor.application.call
import io.ktor.application.log
import io.ktor.auth.authenticate
import io.ktor.http.HttpStatusCode
import io.ktor.locations.*
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.routing.Route

const val TASK_TEMPLATES = "$API_VERSION/task-templates"

@KtorExperimentalLocationsAPI
@Location(TASK_TEMPLATES)
class TaskTemplatesRoute

@KtorExperimentalLocationsAPI
@Location("$TASK_TEMPLATES/{id}")
data class TaskTemplatesIdRoute(val id: Int)

data class TaskTemplateJSON(
    val id: Int,
    val itemTemplateId: Int,
    val taskTemplateDependencyId: Int?,
    val workerTypeId: Int,
    val title: String,
    val timeToComplete: Int,
    val isAdditional: Boolean?,
    val ids: List<Int>?
)

data class TaskTemplateFilterJSON(
    val itemTemplateId: Int?,
    val isAdditional: Boolean?
)

@KtorExperimentalLocationsAPI
fun Route.taskTemplatesRoute(db: TaskTemplateRepository) {
    authenticate("jwt") {
        post<TaskTemplatesRoute> {
            call.getActiveUser(db) ?: return@post

            val jsonData = call.receive<TaskTemplateJSON>()

            val title = jsonData.title
            val itemTemplateId = jsonData.itemTemplateId
            val taskTemplateDependencyId = jsonData.taskTemplateDependencyId
            val workerTypeId = jsonData.workerTypeId
            val timeToComplete = jsonData.timeToComplete
            val isAdditional = jsonData.isAdditional ?: false

            try {
                db.addTaskTemplate(
                    title,
                    itemTemplateId,
                    taskTemplateDependencyId,
                    workerTypeId,
                    timeToComplete,
                    isAdditional
                )?.let { taskTemplate ->
                    taskTemplate.id.let {
                        call.respond(HttpStatusCode.OK, taskTemplate)
                    }
                }
            } catch (e: Throwable) {
                application.log.error("Failed to add TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems adding TaskTemplate")
            }
        }
        get<TaskTemplatesRoute> {
//            call.getActiveUser(db) ?: return@get

            val taskTemplatesParameters = call.request.queryParameters
            val filter = taskTemplatesParameters["filter"]

            val gson = Gson()
            val obj = gson.fromJson(filter, TaskTemplateFilterJSON::class.java)


            try {
                val taskTemplates = if (obj?.itemTemplateId != null && obj.isAdditional != null) {
                    db.getTaskTemplates(obj.itemTemplateId, obj.isAdditional)
                } else if (obj?.itemTemplateId != null) {
                    db.getTaskTemplates(obj.itemTemplateId)
                } else {
                    db.getTaskTemplates()
                }
                call.respond(taskTemplates)
            } catch (e: Throwable) {
                application.log.error("Failed to get TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting TaskTemplate")
            }
        }
        get<TaskTemplatesIdRoute> { params ->
            call.getActiveUser(db) ?: return@get

            try {
                db.findTaskTemplate(params.id)?.let { taskTemplate ->
                    call.respond(taskTemplate)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to get Order", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting Order")
            }
        }
        delete<TaskTemplatesRoute> {
            call.getActiveUser(db) ?: return@delete

            val jsonData = call.receive<TaskTemplateJSON>()

            val ids = jsonData.ids
                ?: return@delete call.respond(HttpStatusCode.BadRequest, "Missing ids")

            try {
                ids.forEach { id ->
                    db.deleteTaskTemplate(id)
                }
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting TaskTemplate")
            }
        }
        put<TaskTemplatesRoute> {
            call.getActiveUser(db) ?: return@put

            val jsonData = call.receive<TaskTemplateJSON>()

            try {
                db.updateTaskTemplate(
                    jsonData.id,
                    jsonData.title,
                    jsonData.itemTemplateId,
                    jsonData.taskTemplateDependencyId,
                    jsonData.workerTypeId,
                    jsonData.timeToComplete,
                    jsonData.isAdditional ?: false
                )?.let {
                    call.respond(it)
                }
            } catch (e: Throwable) {
                application.log.error("Failed to update TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems updating TaskTemplate")
            }
        }
    }
}
