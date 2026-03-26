package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.QueueTypeModel
import QueuingManagementSystem.models.QueueTypeRequest
import QueuingManagementSystem.queries.*

class QueueTypeController {
    fun createQueueType(request: QueuingManagementSystem.models.QueueTypeRequest): Int { QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.postQueueTypeQuery
    ).use { s -> s.setInt(1, request.department_id); s.setString(2, request.name); s.setString(3, request.code); s.setString(4, request.prefix); s.setBoolean(5, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateQueueType(request: QueuingManagementSystem.models.QueueTypeRequest): Boolean { QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.updateQueueTypeQuery
    ).use { s -> s.setString(1, request.name); s.setString(2, request.code); s.setString(3, request.prefix); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getQueueTypesByDepartment(departmentId: Int): MutableList<QueuingManagementSystem.models.QueueTypeModel> { val list = mutableListOf<QueuingManagementSystem.models.QueueTypeModel>(); QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        QueuingManagementSystem.queries.getQueueTypesByDepartmentQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        QueuingManagementSystem.models.QueueTypeModel(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getString("name"),
            rs.getString("code"),
            rs.getString("prefix"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
}
