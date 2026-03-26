package marlow.systems.queuingsystem.controllers

import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.QueueTypeModel
import marlow.systems.queuingsystem.models.QueueTypeRequest
import marlow.systems.queuingsystem.queries.*

class QueueTypeController {
    fun createQueueType(request: QueueTypeRequest): Int { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(postQueueTypeQuery).use { s -> s.setInt(1, request.department_id); s.setString(2, request.name); s.setString(3, request.code); s.setString(4, request.prefix); s.setBoolean(5, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateQueueType(request: QueueTypeRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(updateQueueTypeQuery).use { s -> s.setString(1, request.name); s.setString(2, request.code); s.setString(3, request.prefix); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getQueueTypesByDepartment(departmentId: Int): MutableList<QueueTypeModel> { val list = mutableListOf<QueueTypeModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(getQueueTypesByDepartmentQuery).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(QueueTypeModel(rs.getInt("id"), rs.getInt("department_id"), rs.getString("name"), rs.getString("code"), rs.getString("prefix"), rs.getBoolean("is_active"))) } } }; return list }
}
