package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.ActiveSessionModel
import QueuingManagementSystem.queries.*

class SessionController {
    fun getSessionsForUser(userId: Int): List<ActiveSessionModel> {
        val list = mutableListOf<ActiveSessionModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveSessionsByUserQuery).use { statement ->
                statement.setInt(1, userId)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapRow(rs))
                    }
                }
            }
        }
        return list
    }

    fun getSessionsForAdmin(userId: Int?, departmentId: Int?, limit: Int, offset: Int): List<ActiveSessionModel> {
        val list = mutableListOf<ActiveSessionModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveSessionsByDepartmentQuery).use { statement ->
                if (userId == null) statement.setNull(1, java.sql.Types.INTEGER) else statement.setInt(1, userId)
                if (userId == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, userId)
                if (departmentId == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, departmentId)
                if (departmentId == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, departmentId)
                statement.setInt(5, limit)
                statement.setInt(6, offset)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(mapRow(rs))
                    }
                }
            }
        }
        return list
    }

    fun revokeAnySession(actorUserId: Int, actorDepartmentId: Int?, sessionId: String, reason: String): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val owner = getSessionOwner(connection, sessionId)
                connection.prepareStatement(revokeSessionByIdForAdminQuery).use { statement ->
                    statement.setString(1, reason)
                    statement.setString(2, sessionId)
                    val changed = statement.executeUpdate() > 0
                    if (changed) {
                        connection.prepareStatement(postSessionLifecycleAuditQuery).use { audit ->
                            audit.setInt(1, actorUserId)
                            if (actorDepartmentId == null) audit.setNull(2, java.sql.Types.INTEGER) else audit.setInt(2, actorDepartmentId)
                            audit.setString(3, "FORCED_LOGOUT")
                            audit.setString(4, "user_session")
                            audit.setString(5, sessionId)
                            audit.setString(6, "{\"reason\":\"$reason\",\"owner_user_id\":${owner?.first ?: -1}}")
                            audit.executeUpdate()
                        }
                    }
                    connection.commit()
                    return changed
                }
            } catch (e: Exception) {
                connection.rollback()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun revokeOwnOtherSessions(actorUserId: Int, actorDepartmentId: Int?, currentSessionId: String): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val updated = connection.prepareStatement(revokeUserOtherActiveSessionsQuery).use { statement ->
                    statement.setInt(1, actorUserId)
                    statement.setString(2, currentSessionId)
                    statement.executeUpdate()
                }
                if (updated > 0) {
                    connection.prepareStatement(postSessionLifecycleAuditQuery).use { audit ->
                        audit.setInt(1, actorUserId)
                        if (actorDepartmentId == null) audit.setNull(2, java.sql.Types.INTEGER) else audit.setInt(2, actorDepartmentId)
                        audit.setString(3, "SESSION_REVOKE")
                        audit.setString(4, "user_session")
                        audit.setString(5, "self_other_sessions")
                        audit.setString(6, "{\"reason\":\"USER_REVOKED_OTHER_SESSION\",\"count\":$updated}")
                        audit.executeUpdate()
                    }
                }
                connection.commit()
                return updated
            } catch (e: Exception) {
                connection.rollback()
                return 0
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun canAdminAccessSession(sessionId: String, actorDepartmentId: Int?): Boolean {
        if (actorDepartmentId == null) return true
        ConnectionPoolManager.getConnection().use { connection ->
            val owner = getSessionOwner(connection, sessionId) ?: return false
            return owner.second == actorDepartmentId
        }
    }

    private fun getSessionOwner(connection: java.sql.Connection, sessionId: String): Pair<Int, Int?>? {
        connection.prepareStatement(getSessionOwnerQuery).use { statement ->
            statement.setString(1, sessionId)
            statement.executeQuery().use { rs ->
                if (rs.next()) {
                    return Pair(rs.getInt("user_id"), rs.getInt("department_id").let { if (rs.wasNull()) null else it })
                }
            }
        }
        return null
    }

    private fun mapRow(rs: java.sql.ResultSet): ActiveSessionModel {
        return ActiveSessionModel(
            session_id = rs.getString("session_id"),
            user_id = rs.getInt("user_id"),
            username = rs.getString("username"),
            full_name = rs.getString("full_name"),
            role = rs.getString("role"),
            department_id = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
            token_ref_hash = rs.getString("token_ref_hash"),
            login_at = rs.getString("login_at"),
            last_seen_at = rs.getString("last_seen_at"),
            logout_at = rs.getString("logout_at"),
            ip_address = rs.getString("ip_address"),
            user_agent = rs.getString("user_agent"),
            client_identifier = rs.getString("client_identifier"),
            status = rs.getString("status"),
            revoked_reason = rs.getString("revoked_reason")
        )
    }
}
