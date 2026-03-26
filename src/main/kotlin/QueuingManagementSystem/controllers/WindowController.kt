package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.WindowModel
import QueuingManagementSystem.models.WindowQueueTypeAssignmentRequest
import QueuingManagementSystem.models.WindowRequest
import marlow.systems.queuingsystem.queries.*

class WindowController {
    fun createWindow(request: QueuingManagementSystem.models.WindowRequest): Int { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.postWindowQuery
    ).use { s -> s.setInt(1, request.department_id); if (request.area_id == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, request.area_id); s.setString(3, request.code); s.setString(4, request.name); s.setBoolean(5, request.is_active); s.executeQuery().use { r -> if (r.next()) return r.getInt("id") } } }; return 0 }
    fun updateWindow(request: QueuingManagementSystem.models.WindowRequest): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.updateWindowQuery
    ).use { s -> if (request.area_id == null) s.setNull(1, java.sql.Types.INTEGER) else s.setInt(1, request.area_id); s.setString(2, request.code); s.setString(3, request.name); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getWindowsByDepartment(departmentId: Int): MutableList<QueuingManagementSystem.models.WindowModel> { val list = mutableListOf<QueuingManagementSystem.models.WindowModel>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getWindowsByDepartmentQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        _root_ide_package_.QueuingManagementSystem.models.WindowModel(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getInt("area_id").let { if (rs.wasNull()) null else it },
            rs.getString("code"),
            rs.getString("name"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun assignQueueTypes(request: QueuingManagementSystem.models.WindowQueueTypeAssignmentRequest): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.deleteWindowQueueTypesByWindowQuery
    ).use { s -> s.setInt(1, request.window_id); s.executeUpdate() }; c.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postWindowQueueTypeQuery).use { s -> request.queue_type_ids.forEach { id -> s.setInt(1, request.window_id); s.setInt(2, id); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
}
