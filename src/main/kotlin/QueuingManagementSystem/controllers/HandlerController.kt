package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.HandlerModel
import QueuingManagementSystem.models.HandlerRequest
import marlow.systems.queuingsystem.queries.*

class HandlerController {
    fun createHandler(request: QueuingManagementSystem.models.HandlerRequest): Int { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.postHandlerQuery
    ).use { s -> s.setInt(1, request.user_id); s.setInt(2, request.department_id); s.setBoolean(3, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateHandler(request: QueuingManagementSystem.models.HandlerRequest): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.updateHandlerQuery
    ).use { s -> s.setBoolean(1, request.is_active); s.setInt(2, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getHandlersByDepartment(departmentId: Int): MutableList<QueuingManagementSystem.models.HandlerModel> { val list = mutableListOf<QueuingManagementSystem.models.HandlerModel>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getHandlersByDepartmentQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        _root_ide_package_.QueuingManagementSystem.models.HandlerModel(
            rs.getInt("id"),
            rs.getInt("user_id"),
            rs.getInt("department_id"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun startSession(handlerId: Int, windowId: Int): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.endActiveHandlerSessionQuery
    ).use { s -> s.setInt(1, handlerId); s.executeUpdate() }; c.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postHandlerSessionQuery).use { s -> s.setInt(1, handlerId); s.setInt(2, windowId); s.executeQuery() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
    fun endSession(handlerId: Int): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.endActiveHandlerSessionQuery
    ).use { s -> s.setInt(1, handlerId); return s.executeUpdate() >= 0 } } }
    fun getActiveHandlersForQueueType(queueTypeId: Int): MutableList<Int> { val ids = mutableListOf<Int>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getActiveHandlersForQueueTypeQuery
    ).use { s -> s.setInt(1, queueTypeId); s.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("handler_id")) } } }; return ids }
}
