package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.KioskModel
import QueuingManagementSystem.models.KioskQueueTypeAssignmentRequest
import QueuingManagementSystem.models.KioskRequest
import QueuingManagementSystem.queries.*

class KioskController {
    fun createKiosk(request: KioskRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postKioskQuery).use { statement ->
                statement.setInt(1, request.department_id)
                statement.setString(2, request.name)
                statement.setBoolean(3, request.is_active)
                statement.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("id")
                }
            }
        }
        return 0
    }

    fun updateKiosk(request: KioskRequest): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateKioskQuery).use { statement ->
                statement.setString(1, request.name)
                statement.setBoolean(2, request.is_active)
                statement.setInt(3, request.id ?: 0)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun getKiosks(): MutableList<KioskModel> {
        val list = mutableListOf<KioskModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getKiosksQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            KioskModel(
                                id = rs.getInt("id"),
                                department_id = rs.getInt("department_id"),
                                name = rs.getString("name"),
                                is_active = rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun assignQueueTypes(request: KioskQueueTypeAssignmentRequest): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(deleteKioskQueueTypesByKioskQuery).use { statement ->
                    statement.setInt(1, request.kiosk_id)
                    statement.executeUpdate()
                }
                connection.prepareStatement(postKioskQueueTypeQuery).use { statement ->
                    request.queue_type_ids.forEach { queueTypeId ->
                        statement.setInt(1, request.kiosk_id)
                        statement.setInt(2, queueTypeId)
                        statement.addBatch()
                    }
                    statement.executeBatch()
                }
                connection.commit()
                return true
            } catch (_: Exception) {
                connection.rollback()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getKioskDepartmentById(kioskId: Int): Int? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getKioskDepartmentByIdQuery).use { statement ->
                statement.setInt(1, kioskId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("department_id")
                }
            }
        }
        return null
    }

    fun getQueueTypeDepartmentById(queueTypeId: Int): Int? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getQueueTypeDepartmentByIdQuery).use { statement ->
                statement.setInt(1, queueTypeId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) return rs.getInt("department_id")
                }
            }
        }
        return null
    }
}
