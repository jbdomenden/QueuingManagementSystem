package QueuingManagementSystem.routes

import QueuingManagementSystem.common.extractBearerToken
import QueuingManagementSystem.common.canAccessDepartment
import QueuingManagementSystem.common.Role
import QueuingManagementSystem.common.requireAnyRole
import QueuingManagementSystem.controllers.AuthController
import QueuingManagementSystem.controllers.DisplayController
import QueuingManagementSystem.models.DisplayBoardRequest
import QueuingManagementSystem.models.DisplayBoardWindowAssignmentRequest
import QueuingManagementSystem.models.DisplayFilterParams
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.IdResponse
import QueuingManagementSystem.models.ListResponse
import QueuingManagementSystem.models.UserSessionModel
import QueuingManagementSystem.models.validateDisplayBoardRequest
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import io.ktor.server.routing.RoutingContext

fun Route.displayRoutes() {
    val authController = AuthController()
    val controller = DisplayController()

    suspend fun RoutingContext.requireDisplayPermission(session: UserSessionModel, permission: String): Boolean {
        if (!session.permissions.contains(permission)) {
            call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Missing permission: $permission"))
            return false
        }
        return true
    }

    route("/displays") {
        post("/create") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@post
                if (!session.permissions.contains("display_manage")) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<DisplayBoardRequest>()
                val errors = request.validateDisplayBoardRequest()
                if (errors.isNotEmpty()) return@post call.respond(HttpStatusCode.BadRequest, errors)
                if (!session.canAccessDepartment(request.department_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                val newId = controller.createDisplayBoard(request)
                call.respond(HttpStatusCode.OK, IdResponse(newId, GlobalCredentialResponse(200, true, "Display created")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        put("/update") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@put
                if (!session.permissions.contains("display_manage")) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<DisplayBoardRequest>()
                if (!session.canAccessDepartment(request.department_id)) {
                    return@put call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }
                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.updateDisplayBoard(request), "Display updated"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        post("/assign-windows") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireAnyRole(session.role, setOf(Role.SUPER_ADMIN, Role.DEPARTMENT_ADMIN))) return@post
                if (!session.permissions.contains("display_manage")) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Forbidden"))
                }

                val request = call.receive<DisplayBoardWindowAssignmentRequest>()
                val display = controller.getDisplayBoardById(request.display_board_id)
                    ?: return@post call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Display not found"))

                if (!session.canAccessDepartment(display.department_id)) {
                    return@post call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Department scope violation"))
                }

                call.respond(HttpStatusCode.OK, GlobalCredentialResponse(200, controller.assignWindows(request), "Display windows assigned"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }


        get("/wallboard/{displayId}") {
            try {
                val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0
                if (displayId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "displayId is required"))
                }
                val display = controller.getDisplayBoardById(displayId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Display not found"))
                if (!display.is_active) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Display is inactive"))
                }
                val selectedFilter = call.request.queryParameters["filter"]
                call.respond(HttpStatusCode.OK, controller.getDisplayWallboard(displayId, selectedFilter))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/snapshot/{displayId}") {
            try {
                val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0
                if (displayId <= 0) {
                    return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "displayId is required"))
                }
                val display = controller.getDisplayBoardById(displayId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Display not found"))
                if (!display.is_active) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Display is inactive"))
                }
                call.respond(HttpStatusCode.OK, controller.getDisplaySnapshot(displayId))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/aggregate/{displayId}") {
            try {
                val displayId = call.parameters["displayId"]?.toIntOrNull() ?: 0
                if (displayId <= 0) return@get call.respond(HttpStatusCode.BadRequest, GlobalCredentialResponse(400, false, "displayId is required"))
                val display = controller.getDisplayBoardById(displayId)
                    ?: return@get call.respond(HttpStatusCode.NotFound, GlobalCredentialResponse(404, false, "Display not found"))
                if (!display.is_active) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Display is inactive"))
                }

                val filters = DisplayFilterParams(
                    department_id = call.request.queryParameters["department_id"]?.toIntOrNull(),
                    area_id = call.request.queryParameters["area_id"]?.toIntOrNull(),
                    floor_id = call.request.queryParameters["floor_id"]?.toIntOrNull(),
                    company_id = call.request.queryParameters["company_id"]?.toIntOrNull()
                )
                call.respond(HttpStatusCode.OK, controller.getDisplayAggregateSnapshot(displayId, filters))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }

        get("/list") {
            try {
                val session = authController.getUserSessionByToken(call.request.extractBearerToken())
                if (!requireDisplayPermission(session, "display_view")) return@get
                val hasDepartmentScope = session.permissions.contains("display_scope_department")
                val hasGlobalScope = session.permissions.contains("display_scope_global")
                if (!hasDepartmentScope && !hasGlobalScope) {
                    return@get call.respond(HttpStatusCode.Forbidden, GlobalCredentialResponse(403, false, "Missing display scope permission"))
                }
                val boards = controller.getDisplayBoards().filter {
                    hasGlobalScope || (hasDepartmentScope && session.department_id == it.department_id)
                }
                call.respond(HttpStatusCode.OK, ListResponse(boards, GlobalCredentialResponse(200, true, "OK")))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, GlobalCredentialResponse(500, false, e.message ?: "Internal server error"))
            }
        }
    }
}
