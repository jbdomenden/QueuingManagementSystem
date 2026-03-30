package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.QueueTypeModel
import QueuingManagementSystem.models.QueueTypeRequest
import QueuingManagementSystem.queries.*

class QueueTypeController {
    fun createQueueType(request: QueueTypeRequest): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(postQueueTypeQuery).use { s ->
                s.setInt(1, request.department_id)
                if (request.company_id == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, request.company_id)
                s.setString(3, request.name)
                s.setString(4, request.code)
                s.setString(5, request.prefix)
                s.setBoolean(6, request.is_active)
                s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateQueueType(request: QueueTypeRequest): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(updateQueueTypeQuery).use { s ->
                s.setInt(1, request.department_id)
                if (request.company_id == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, request.company_id)
                s.setString(3, request.name)
                s.setString(4, request.code)
                s.setString(5, request.prefix)
                s.setBoolean(6, request.is_active)
                s.setInt(7, request.id ?: 0)
                return s.executeUpdate() > 0
            }
        }
    }

    fun getQueueTypesByDepartment(departmentId: Int): MutableList<QueueTypeModel> {
        val list = mutableListOf<QueueTypeModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getQueueTypesByDepartmentQuery).use { s ->
                s.setInt(1, departmentId)
                s.executeQuery().use { rs -> while (rs.next()) list.add(mapQueueType(rs)) }
            }
        }
        return list
    }

    fun getQueueTypesByCompanyId(companyId: Int): MutableList<QueueTypeModel> {
        val list = mutableListOf<QueueTypeModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getQueueTypesByCompanyIdQuery).use { s ->
                s.setInt(1, companyId)
                s.executeQuery().use { rs -> while (rs.next()) list.add(mapQueueType(rs)) }
            }
        }
        return list
    }

    private fun mapQueueType(rs: java.sql.ResultSet): QueueTypeModel {
        return QueueTypeModel(
            id = rs.getInt("id"),
            department_id = rs.getInt("department_id"),
            company_id = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
            name = rs.getString("name"),
            code = rs.getString("code"),
            prefix = rs.getString("prefix"),
            is_active = rs.getBoolean("is_active"),
            kiosk_id = rs.getInt("kiosk_id").let { if (rs.wasNull()) null else it }
        )
    }
}
