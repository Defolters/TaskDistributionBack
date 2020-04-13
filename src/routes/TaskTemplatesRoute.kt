package io.defolters.routes

import io.defolters.API_VERSION
import io.defolters.repository.interfaces.ItemTemplateRepository
import io.defolters.repository.interfaces.TaskTemplateRepository
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

const val TASK_TEMPLATES = "$API_VERSION/task-templates"

@KtorExperimentalLocationsAPI
@Location(TASK_TEMPLATES)
class TaskTemplatesRoute

@KtorExperimentalLocationsAPI
fun Route.taskTemplates(db: TaskTemplateRepository) {
    authenticate("jwt") {
        post<TaskTemplatesRoute> {
            call.getActiveUser(db) ?: return@post

            val taskTemplateParameters = call.receive<Parameters>()

            val title = taskTemplateParameters["title"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing title")
            val itemTemplateId = taskTemplateParameters["itemTemplateId"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing itemTemplateId")
            val taskTemplateDependencyId = taskTemplateParameters["taskTemplateDependencyId"]
            val workerType = taskTemplateParameters["workerType"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing workerType")
            val timeToComplete = taskTemplateParameters["timeToComplete"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing timeToComplete")
            val isAdditional = taskTemplateParameters["isAdditional"]
                ?: return@post call.respond(HttpStatusCode.BadRequest, "Missing isAdditional")

            if ((db as? ItemTemplateRepository)?.findItemTemplate(itemTemplateId.toIntOrNull()) == null) {
                return@post call.respond(
                    HttpStatusCode.BadRequest,
                    "Not valid itemTemplateId"
                )
            }

            try {
                db.addTaskTemplate(
                    title,
                    itemTemplateId.toInt(),
                    taskTemplateDependencyId?.toInt(),
                    workerType,
                    timeToComplete.toInt(),
                    isAdditional.toBoolean()
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
            call.getActiveUser(db) ?: return@get

            val taskTemplatesParameters = call.request.queryParameters
            val itemTemplateId = taskTemplatesParameters["itemTemplateId"]

            try {
                val taskTemplates = if (itemTemplateId != null) {
                    db.getTaskTemplates(itemTemplateId.toInt())
                } else {
                    db.getTaskTemplates()
                }
                call.respond(taskTemplates)
            } catch (e: Throwable) {
                application.log.error("Failed to get TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems getting TaskTemplate")
            }
        }
        delete<TaskTemplatesRoute> {
            call.getActiveUser(db) ?: return@delete

            val taskTemplateParameters = call.receive<Parameters>()
            val taskTemplateId = taskTemplateParameters["id"]
                ?: return@delete call.respond(
                    HttpStatusCode.BadRequest, "Missing itemTemplate Id"
                )

            try {
                db.deleteTaskTemplate(taskTemplateId.toInt())
                call.respond(HttpStatusCode.OK)
            } catch (e: Throwable) {
                application.log.error("Failed to delete TaskTemplate", e)
                call.respond(HttpStatusCode.BadRequest, "Problems Deleting TaskTemplate")
            }
        }
    }
}
