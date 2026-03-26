package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.WindowModel
import QueuingManagementSystem.models.WindowQueueTypeAssignmentRequest
import QueuingManagementSystem.models.WindowRequest
import QueuingManagementSystem.queries.deleteWindowQueueTypesByWindowQuery
import QueuingManagementSystem.queries.getWindowsByDepartmentQuery
import QueuingManagementSystem.queries.postWindowQuery
import QueuingManagementSystem.queries.postWindowQueueTypeQuery
import QueuingManagementSystem.queries.updateWindowQuery
import java.sql.Types
import kotlin.compareTo

class WindowController {
    fun createWindow(request: WindowRequest): Int { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        postWindowQuery
    ).use { s -> s.setInt(1, request.department_id); if (request.area_id == null) s.setNull(2, Types.INTEGER) else s.setInt(2, request.area_id); s.setString(3, request.code); s.setString(4, request.name); s.setBoolean(5, request.is_active); s.executeQuery().use { r -> if (r.next()) return r.getInt("id") } } }; return 0 }
    fun updateWindow(request: WindowRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        updateWindowQuery
    ).use { s -> if (request.area_id == null) s.setNull(1, Types.INTEGER) else s.setInt(1, request.area_id); s.setString(2, request.code); s.setString(3, request.name); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() compareTo 0 } } }
    fun getWindowsByDepartment(departmentId: Int): MutableList<WindowModel> { val list = mutableListOf<WindowModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getWindowsByDepartmentQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        WindowModel(
            rs.getInt(
                "id"
            ),
            rs.getInt("department_id"),
            rs.getInt("area_id").let { if (rs.wasNull()) null else it },
            rs.getString("code"),
            rs.getString("name"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun assignQueueTypes(request: WindowQueueTypeAssignmentRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        deleteWindowQueueTypesByWindowQuery
    ).use { s -> s.setInt(1, request.window_id); s.executeUpdate() }; c.prepareStatement(postWindowQueueTypeQuery).use { s -> request.queue_type_ids.forEach { id -> s.setInt(1, request.window_id); s.setInt(2, id); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
}
