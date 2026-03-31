package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.WorkflowTemplateController
import QueuingManagementSystem.models.*
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.*

fun Route.workflowTemplateRoutes() {
    val authController = AuthController()
    val controller = WorkflowTemplateController()

    suspend fun RoutingContext.requireWorkflowPermission(session: AuthController.ValidatedSession, permission: String): Boolean {
        if (!session.permissions.contains(permission)) {
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Missing permission: $permission"))
            return false
        }
        return true
    }

    route("/workflow-templates") {
        post("/create") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_manage")) return@post

            val request = call.receive<WorkflowTemplateRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            val created = controller.createTemplate(request, session.userId)
                ?: return@post call.respond(HttpStatusCode.BadRequest, WorkflowTemplateResponse(null, GlobalCredentialResponse(400, false, "Template create failed")))
            call.respond(HttpStatusCode.OK, WorkflowTemplateResponse(created, GlobalCredentialResponse(200, true, "Template created")))
        }

        put("/update") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@put call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_manage")) return@put

            val request = call.receive<WorkflowTemplateRequest>()
            if (request.id == null || request.id <= 0) return@put call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "id is required"))
            val errors = request.validate()
            if (errors.isNotEmpty()) return@put call.respond(HttpStatusCode.BadRequest, errors)
            val updated = controller.updateTemplate(request, session.userId)
                ?: return@put call.respond(HttpStatusCode.BadRequest, WorkflowTemplateResponse(null, GlobalCredentialResponse(400, false, "Template update failed")))
            call.respond(HttpStatusCode.OK, WorkflowTemplateResponse(updated, GlobalCredentialResponse(200, true, "Template updated")))
        }

        post("/assign") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_assign")) return@post

            val request = call.receive<WorkflowAssignmentRequest>()
            val errors = request.validate()
            if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
            val assignment = controller.assignTemplate(request, session.userId)
                ?: return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "Template assignment failed"))
            call.respond(HttpStatusCode.OK, SingleResponse(assignment, GlobalCredentialResponse(200, true, "Template assigned")))
        }

        post("/toggle") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@post call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_manage")) return@post

            val request = call.receive<WorkflowTemplateToggleRequest>()
            if (request.template_id <= 0) return@post call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "template_id is required"))
            val changed = controller.setTemplateEnabled(request.template_id, request.enabled, session.userId)
            call.respond(if (changed) HttpStatusCode.OK else HttpStatusCode.BadRequest, GlobalCredentialResponse(if (changed) 200 else 400, changed, if (changed) "Template state updated" else "Template not found"))
        }

        get("/list") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_view")) return@get

            val includeInactive = call.request.queryParameters["include_inactive"]?.toBoolean() ?: false
            call.respond(HttpStatusCode.OK, ListResponse(controller.listTemplates(includeInactive), GlobalCredentialResponse(200, true, "OK")))
        }

        get("/active") {
            val session = authController.getValidatedSessionByToken(call.request.extractBearerToken())
                ?: return@get call.respond(HttpStatusCode.Unauthorized, GlobalCredentialResponse(401, false, "Unauthorized"))
            if (!requireWorkflowPermission(session, "workflow_template_view")) return@get

            val departmentId = call.request.queryParameters["department_id"]?.toIntOrNull()
                ?: return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "department_id is required"))
            val lookup = WorkflowActiveLookupRequest(
                department_id = departmentId,
                queue_type_id = call.request.queryParameters["queue_type_id"]?.toIntOrNull(),
                company_id = call.request.queryParameters["company_id"]?.toIntOrNull(),
                company_transaction_id = call.request.queryParameters["company_transaction_id"]?.toIntOrNull(),
                transaction_family = call.request.queryParameters["transaction_family"]
            )
            val active = controller.getActiveTemplate(lookup)
            if (active == null) {
                call.respond(HttpStatusCode.NotFound, WorkflowTemplateResponse(null, GlobalCredentialResponse(404, false, "No active workflow template")))
            } else {
                call.respond(HttpStatusCode.OK, WorkflowTemplateResponse(active, GlobalCredentialResponse(200, true, "OK")))
            }
        }
    }
}
