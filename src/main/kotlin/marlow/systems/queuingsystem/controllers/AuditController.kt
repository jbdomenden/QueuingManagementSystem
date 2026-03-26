package marlow.systems.queuingsystem.controllers

import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.AuditLogModel
import marlow.systems.queuingsystem.queries.*

class AuditController {
    fun createAuditLog(actorUserId: Int?, departmentId: Int?, action: String, entityName: String, entityId: String, payloadJson: String?): Boolean {
        ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(postAuditLogQuery).use { s -> if (actorUserId == null) s.setNull(1, java.sql.Types.INTEGER) else s.setInt(1, actorUserId); if (departmentId == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, departmentId); s.setString(3, action); s.setString(4, entityName); s.setString(5, entityId); s.setString(6, payloadJson); return s.executeUpdate() > 0 } }
    }
    fun getAuditLogs(departmentId: Int?): MutableList<AuditLogModel> { val list = mutableListOf<AuditLogModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(if (departmentId == null) getAuditLogsQuery else getAuditLogsByDepartmentQuery).use { s -> if (departmentId != null) s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(AuditLogModel(rs.getInt("id"), rs.getInt("actor_user_id").let { if (rs.wasNull()) null else it }, rs.getInt("department_id").let { if (rs.wasNull()) null else it }, rs.getString("action"), rs.getString("entity_name"), rs.getString("entity_id"), rs.getString("payload_json"), rs.getString("created_at"))) } } }; return list }
}
