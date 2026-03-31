package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.HandlerModel
import QueuingManagementSystem.models.HandlerRequest
import QueuingManagementSystem.models.HandlerSessionModel
import QueuingManagementSystem.queries.*

class HandlerController {
    fun createHandler(request: HandlerRequest): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(postHandlerQuery).use { s ->
                s.setInt(1, request.user_id)
                s.setInt(2, request.department_id)
                s.setBoolean(3, request.is_active)
                s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateHandler(request: HandlerRequest): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(updateHandlerQuery).use { s ->
                s.setBoolean(1, request.is_active)
                s.setInt(2, request.id ?: 0)
                return s.executeUpdate() > 0
            }
        }
    }

    fun getHandlersByDepartment(departmentId: Int): MutableList<HandlerModel> {
        val list = mutableListOf<HandlerModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getHandlersByDepartmentQuery).use { s ->
                s.setInt(1, departmentId)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(HandlerModel(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("department_id"), rs.getBoolean("is_active")))
                    }
                }
            }
        }
        return list
    }

    fun startSession(handlerId: Int, windowId: Int): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.autoCommit = false
            try {
                var userId = 0
                c.prepareStatement(validateHandlerWindowDepartmentQuery).use { s ->
                    s.setInt(1, windowId)
                    s.setInt(2, handlerId)
                    s.executeQuery().use { rs -> if (rs.next()) userId = rs.getInt("id") }
                }
                if (userId <= 0) {
                    c.rollback()
                    return false
                }

                c.prepareStatement(endActiveHandlerSessionQuery).use { s ->
                    s.setInt(1, handlerId)
                    s.executeUpdate()
                }

                c.prepareStatement("SELECT user_id FROM handlers WHERE id = ? AND is_active = true").use { s ->
                    s.setInt(1, handlerId)
                    s.executeQuery().use { rs -> if (rs.next()) userId = rs.getInt("user_id") }
                }
                if (userId <= 0) {
                    c.rollback()
                    return false
                }

                c.prepareStatement(postHandlerSessionQuery).use { s ->
                    s.setInt(1, handlerId)
                    s.setInt(2, userId)
                    s.setInt(3, windowId)
                    s.executeQuery()
                }

                c.commit()
                return true
            } catch (e: Exception) {
                c.rollback()
                return false
            } finally {
                c.autoCommit = true
            }
        }
    }

    fun endSession(handlerId: Int): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(endActiveHandlerSessionQuery).use { s ->
                s.setInt(1, handlerId)
                return s.executeUpdate() > 0
            }
        }
    }

    fun getActiveSession(handlerId: Int): HandlerSessionModel? {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getActiveHandlerSessionByHandlerIdQuery).use { s ->
                s.setInt(1, handlerId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        return HandlerSessionModel(
                            rs.getInt("id"),
                            rs.getInt("handler_id"),
                            rs.getInt("user_id"),
                            rs.getInt("window_id"),
                            rs.getString("login_time"),
                            rs.getString("last_seen_at"),
                            rs.getString("status")
                        )
                    }
                }
            }
        }
        return null
    }


    fun getActiveHandlerByUserId(userId: Int): HandlerModel? {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getHandlerByUserIdQuery).use { s ->
                s.setInt(1, userId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        return HandlerModel(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("department_id"), rs.getBoolean("is_active"))
                    }
                }
            }
        }
        return null
    }

    fun getHandlerById(handlerId: Int): HandlerModel? {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getHandlerByIdQuery).use { s ->
                s.setInt(1, handlerId)
                s.executeQuery().use { rs ->
                    if (rs.next()) return HandlerModel(rs.getInt("id"), rs.getInt("user_id"), rs.getInt("department_id"), rs.getBoolean("is_active"))
                }
            }
        }
        return null
    }

    fun getActiveHandlersForQueueType(queueTypeId: Int): MutableList<Int> {
        val ids = mutableListOf<Int>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getActiveHandlersForQueueTypeQuery).use { s ->
                s.setInt(1, queueTypeId)
                s.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("handler_id")) }
            }
        }
        return ids
    }
}
