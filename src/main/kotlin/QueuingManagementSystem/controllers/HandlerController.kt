package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.HandlerModel
import QueuingManagementSystem.models.HandlerRequest
import QueuingManagementSystem.queries.endActiveHandlerSessionQuery
import QueuingManagementSystem.queries.getActiveHandlersForQueueTypeQuery
import QueuingManagementSystem.queries.getHandlersByDepartmentQuery
import QueuingManagementSystem.queries.postHandlerQuery
import QueuingManagementSystem.queries.postHandlerSessionQuery
import QueuingManagementSystem.queries.updateHandlerQuery
import kotlin.compareTo

class HandlerController {
    fun createHandler(request: HandlerRequest): Int { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        postHandlerQuery
    ).use { s -> s.setInt(1, request.user_id); s.setInt(2, request.department_id); s.setBoolean(3, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateHandler(request: HandlerRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        updateHandlerQuery
    ).use { s -> s.setBoolean(1, request.is_active); s.setInt(2, request.id ?: 0); return s.executeUpdate() compareTo 0 } } }
    fun getHandlersByDepartment(departmentId: Int): MutableList<HandlerModel> { val list = mutableListOf<HandlerModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getHandlersByDepartmentQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        HandlerModel(
            rs.getInt(
                "id"
            ), rs.getInt("user_id"), rs.getInt("department_id"), rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun startSession(handlerId: Int, windowId: Int): Boolean { ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        endActiveHandlerSessionQuery
    ).use { s -> s.setInt(1, handlerId); s.executeUpdate() }; c.prepareStatement(postHandlerSessionQuery).use { s -> s.setInt(1, handlerId); s.setInt(2, windowId); s.executeQuery() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
    fun endSession(handlerId: Int): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        endActiveHandlerSessionQuery
    ).use { s -> s.setInt(1, handlerId); return s.executeUpdate() compareTo 0 } } }
    fun getActiveHandlersForQueueType(queueTypeId: Int): MutableList<Int> { val ids = mutableListOf<Int>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getActiveHandlersForQueueTypeQuery
    ).use { s -> s.setInt(1, queueTypeId); s.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("handler_id")) } } }; return ids }
}
