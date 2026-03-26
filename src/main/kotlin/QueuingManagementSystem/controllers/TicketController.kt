package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.TicketCreateRequest
import QueuingManagementSystem.models.TicketModel
import QueuingManagementSystem.queries.*

class TicketController {
    fun createTicket(request: TicketCreateRequest): TicketModel {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var departmentId = 0
                var prefix = ""
                connection.prepareStatement(getQueueTypeWithDepartmentByIdQuery).use { statement ->
                    statement.setInt(1, request.queue_type_id)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            departmentId = rs.getInt("department_id")
                            prefix = rs.getString("prefix")
                        }
                    }
                }
                if (departmentId == 0) throw IllegalStateException("queue type not found")

                var kioskAllowed = false
                connection.prepareStatement(getKioskByIdAndDepartmentQueueTypeQuery).use { statement ->
                    statement.setInt(1, request.kiosk_id)
                    statement.setInt(2, request.queue_type_id)
                    statement.executeQuery().use { rs -> kioskAllowed = rs.next() }
                }
                if (!kioskAllowed) throw IllegalStateException("kiosk is not mapped to queue type")

                var sequence = 0
                connection.prepareStatement(upsertQueueDailySequenceQuery).use { statement ->
                    statement.setInt(1, request.queue_type_id)
                    statement.executeQuery().use { rs -> if (rs.next()) sequence = rs.getInt("current_value") }
                }

                val ticketNumber = "$prefix-${sequence.toString().padStart(3, '0')}"
                var ticket = TicketModel(0, "", 0, 0, null, null, null, "", "")
                connection.prepareStatement(postTicketQuery).use { statement ->
                    statement.setString(1, ticketNumber)
                    statement.setInt(2, departmentId)
                    statement.setInt(3, request.queue_type_id)
                    statement.setInt(4, request.kiosk_id)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            ticket = mapTicket(rs)
                        }
                    }
                }

                if (ticket.id <= 0) throw IllegalStateException("failed to insert ticket")

                connection.prepareStatement(postTicketLogQuery).use { statement ->
                    statement.setInt(1, ticket.id)
                    statement.setString(2, "CREATED")
                    statement.setNull(3, java.sql.Types.INTEGER)
                    statement.setString(4, "{\"kiosk_id\":${request.kiosk_id}}")
                    statement.executeUpdate()
                }

                connection.commit()
                return ticket
            } catch (e: Exception) {
                connection.rollback()
                return TicketModel(0, "", 0, 0, null, null, null, "", "")
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getLiveTickets(departmentId: Int): MutableList<TicketModel> {
        val list = mutableListOf<TicketModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getLiveTicketsByDepartmentQuery).use { statement ->
                statement.setInt(1, departmentId)
                statement.executeQuery().use { rs ->
                    while (rs.next()) list.add(mapTicket(rs))
                }
            }
        }
        return list
    }

    fun callNext(handlerId: Int): TicketModel {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(getWaitingTicketForHandlerCallNextWithLockingQuery).use { statement ->
                    statement.setInt(1, handlerId)
                    statement.setInt(2, handlerId)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            val model = mapTicket(rs)
                            connection.prepareStatement(postTicketLogQuery).use { log ->
                                log.setInt(1, model.id)
                                log.setString(2, "CALLED")
                                log.setInt(3, handlerId)
                                log.setString(4, "{\"source\":\"call_next\"}")
                                log.executeUpdate()
                            }
                            connection.commit()
                            return model
                        }
                    }
                }
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
            } finally {
                connection.autoCommit = true
            }
        }
        return TicketModel(0, "", 0, 0, null, null, null, "", "")
    }

    fun updateTicketStatus(ticketId: Int, handlerId: Int, action: String, notes: String? = null): Boolean {
        val payload = notes?.replace("\"", "'") ?: ""
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val updated = when (action) {
                    "IN_SERVICE" -> runUpdate(connection, updateTicketToInServiceQuery, ticketId, handlerId) || runUpdateForCurrentWindow(connection, ticketId, handlerId, "IN_SERVICE")
                    "SKIPPED" -> runUpdate(connection, updateTicketToSkippedQuery, ticketId, handlerId)
                    "CALLED" -> runUpdate(connection, updateTicketToCalledRecallQuery, ticketId, handlerId)
                    "COMPLETED" -> runUpdate(connection, updateTicketToCompletedQuery, ticketId, handlerId)
                    else -> false
                }
                if (!updated) {
                    connection.rollback()
                    return false
                }

                connection.prepareStatement(postTicketLogQuery).use { statement ->
                    statement.setInt(1, ticketId)
                    statement.setString(2, action)
                    statement.setInt(3, handlerId)
                    statement.setString(4, "{\"notes\":\"$payload\"}")
                    statement.executeUpdate()
                }
                connection.commit()
                return true
            } catch (e: Exception) {
                connection.rollback()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getDisplayIdsForQueueType(queueTypeId: Int): MutableList<Int> {
        val ids = mutableListOf<Int>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getDisplayIdsForQueueTypeQuery).use { statement ->
                statement.setInt(1, queueTypeId)
                statement.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("display_board_id")) }
            }
        }
        return ids
    }


    fun getDisplayIdsByHandler(handlerId: Int): MutableList<Int> {
        val ids = mutableListOf<Int>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getDisplayIdsByHandlerQuery).use { statement ->
                statement.setInt(1, handlerId)
                statement.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("display_board_id")) }
            }
        }
        return ids
    }

    private fun runUpdate(connection: java.sql.Connection, query: String, ticketId: Int, handlerId: Int): Boolean {
        connection.prepareStatement(query).use { statement ->
            statement.setInt(1, ticketId)
            statement.setInt(2, handlerId)
            return statement.executeUpdate() > 0
        }
    }


    fun getDisplayIdsByHandler(handlerId: Int): MutableList<Int> {
        val ids = mutableListOf<Int>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getDisplayIdsByHandlerQuery).use { statement ->
                statement.setInt(1, handlerId)
                statement.executeQuery().use { rs -> while (rs.next()) ids.add(rs.getInt("display_board_id")) }
            }
        }
        return ids
    }

    private fun runUpdateForCurrentWindow(connection: java.sql.Connection, ticketId: Int, handlerId: Int, action: String): Boolean {
        val query = """
UPDATE tickets
SET status = ?,
    service_started_at = CASE WHEN ? = 'IN_SERVICE' THEN NOW() ELSE service_started_at END,
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_window_id = (
      SELECT window_id FROM handler_sessions WHERE handler_id = ? AND is_active = true ORDER BY id DESC LIMIT 1
  )
  AND status IN ('CALLED', 'IN_SERVICE')
"""
        connection.prepareStatement(query).use { statement ->
            statement.setString(1, action)
            statement.setString(2, action)
            statement.setInt(3, ticketId)
            statement.setInt(4, handlerId)
            return statement.executeUpdate() > 0
        }
    }

    private fun mapTicket(rs: java.sql.ResultSet): TicketModel {
        return TicketModel(
            id = rs.getInt("id"),
            ticket_number = rs.getString("ticket_number"),
            department_id = rs.getInt("department_id"),
            queue_type_id = rs.getInt("queue_type_id"),
            kiosk_id = rs.getInt("kiosk_id").let { if (rs.wasNull()) null else it },
            assigned_window_id = rs.getInt("assigned_window_id").let { if (rs.wasNull()) null else it },
            assigned_handler_id = rs.getInt("assigned_handler_id").let { if (rs.wasNull()) null else it },
            status = rs.getString("status"),
            created_at = rs.getString("created_at"),
            called_at = rs.getString("called_at"),
            completed_at = rs.getString("completed_at")
        )
    }
}
