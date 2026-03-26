package QueuingManagementSystem.controllers

import QueuingManagementSystem.common.formatDurationToHms
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
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
                    statement.executeQuery().use { rs -> if (rs.next()) ticket = mapTicket(rs) }
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

    fun createTicketWithPrintable(request: TicketCreateRequest): TicketCreateResponse {
        val ticket = createTicket(request)
        if (ticket.id <= 0) {
            return TicketCreateResponse(
                ticket,
                PrintableTicketModel(0, "", 0, "", 0, "", "", "", "", "", ""),
                GlobalCredentialResponse(400, false, "Ticket create failed")
            )
        }

        val printable = getPrintableTicketDetails(ticket.id)
            ?: PrintableTicketModel(
                ticket.id,
                ticket.ticket_number,
                ticket.department_id,
                "",
                ticket.queue_type_id,
                "",
                ticket.status,
                ticket.queueDate ?: "",
                ticket.queueTime ?: "",
                ticket.queuedAt ?: ticket.created_at,
                "Queue Number: ${ticket.ticket_number}"
            )

        return TicketCreateResponse(ticket, printable, GlobalCredentialResponse(200, true, "Ticket created"))
    }

    fun getPrintableTicketDetails(ticketIdParam: Int): PrintableTicketModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getTicketPrintableDetailsByIdQuery).use { statement ->
                statement.setInt(1, ticketIdParam)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        val formatted = buildString {
                            appendLine(rs.getString("department_name"))
                            appendLine("Queue Number: ${rs.getString("ticket_number")}")
                            appendLine("Queue Type: ${rs.getString("queue_type_name")}")
                            appendLine("Date: ${rs.getString("queue_date")}")
                            append("Time: ${rs.getString("queue_time")}\nPlease wait for your number to be called")
                        }
                        return PrintableTicketModel(
                            ticketId = rs.getInt("ticket_id"),
                            ticketNumber = rs.getString("ticket_number"),
                            departmentId = rs.getInt("department_id"),
                            departmentName = rs.getString("department_name"),
                            queueTypeId = rs.getInt("queue_type_id"),
                            queueTypeName = rs.getString("queue_type_name"),
                            status = rs.getString("status"),
                            queueDate = rs.getString("queue_date"),
                            queueTime = rs.getString("queue_time"),
                            queuedAt = rs.getString("queued_at"),
                            formattedPrintText = formatted
                        )
                    }
                }
            }
        }
        return null
    }

    fun archiveQueuesByServiceDate(serviceDateParam: String, actorUserIdParam: Int, departmentIdParam: Int? = null): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var affected = 0
                connection.prepareStatement(markTicketsArchivedByServiceDateQuery).use { statement ->
                    statement.setString(1, serviceDateParam)
                    if (departmentIdParam == null) {
                        statement.setNull(2, java.sql.Types.INTEGER)
                        statement.setNull(3, java.sql.Types.INTEGER)
                    } else {
                        statement.setInt(2, departmentIdParam)
                        statement.setInt(3, departmentIdParam)
                    }
                    affected = statement.executeUpdate()
                }
                connection.commit()
                return affected
            } catch (e: Exception) {
                connection.rollback()
                return 0
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getArchivedTicketsByDepartment(departmentIdParam: Int, dateFromParam: String, dateToParam: String, queueTypeIdParam: Int?, statusParam: String?): MutableList<ArchivedTicketModel> {
        val list = mutableListOf<ArchivedTicketModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getArchivedTicketsByDepartmentAndDateRangeQuery).use { statement ->
                statement.setInt(1, departmentIdParam)
                statement.setString(2, dateFromParam)
                statement.setString(3, dateToParam)
                if (queueTypeIdParam == null) {
                    statement.setNull(4, java.sql.Types.INTEGER)
                    statement.setNull(5, java.sql.Types.INTEGER)
                } else {
                    statement.setInt(4, queueTypeIdParam)
                    statement.setInt(5, queueTypeIdParam)
                }
                if (statusParam.isNullOrBlank()) {
                    statement.setNull(6, java.sql.Types.VARCHAR)
                    statement.setNull(7, java.sql.Types.VARCHAR)
                } else {
                    statement.setString(6, statusParam)
                    statement.setString(7, statusParam)
                }
                statement.executeQuery().use { rs -> while (rs.next()) list.add(mapArchivedTicket(rs)) }
            }
        }
        return list
    }

    fun getArchivedTickets(dateFromParam: String, dateToParam: String, queueTypeIdParam: Int?, statusParam: String?): MutableList<ArchivedTicketModel> {
        val list = mutableListOf<ArchivedTicketModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getArchivedTicketsByDateRangeQuery).use { statement ->
                statement.setString(1, dateFromParam)
                statement.setString(2, dateToParam)
                if (queueTypeIdParam == null) {
                    statement.setNull(3, java.sql.Types.INTEGER)
                    statement.setNull(4, java.sql.Types.INTEGER)
                } else {
                    statement.setInt(3, queueTypeIdParam)
                    statement.setInt(4, queueTypeIdParam)
                }
                if (statusParam.isNullOrBlank()) {
                    statement.setNull(5, java.sql.Types.VARCHAR)
                    statement.setNull(6, java.sql.Types.VARCHAR)
                } else {
                    statement.setString(5, statusParam)
                    statement.setString(6, statusParam)
                }
                statement.executeQuery().use { rs -> while (rs.next()) list.add(mapArchivedTicket(rs)) }
            }
        }
        return list
    }

    fun getArchivedTicketById(ticketId: Int): ArchivedTicketModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getArchivedTicketByIdQuery).use { statement ->
                statement.setInt(1, ticketId)
                statement.executeQuery().use { rs -> if (rs.next()) return mapArchivedTicket(rs) }
            }
        }
        return null
    }

    fun getLiveTickets(departmentId: Int): MutableList<TicketModel> {
        val list = mutableListOf<TicketModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getLiveTicketsByDepartmentQuery).use { statement ->
                statement.setInt(1, departmentId)
                statement.executeQuery().use { rs -> while (rs.next()) list.add(mapTicket(rs)) }
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
  AND archived = false
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
        val createdAt = rs.getString("created_at")
        return TicketModel(
            id = rs.getInt("id"),
            ticket_number = rs.getString("ticket_number"),
            department_id = rs.getInt("department_id"),
            queue_type_id = rs.getInt("queue_type_id"),
            kiosk_id = rs.getInt("kiosk_id").let { if (rs.wasNull()) null else it },
            assigned_window_id = rs.getInt("assigned_window_id").let { if (rs.wasNull()) null else it },
            assigned_handler_id = rs.getInt("assigned_handler_id").let { if (rs.wasNull()) null else it },
            status = rs.getString("status"),
            created_at = createdAt,
            called_at = rs.getString("called_at"),
            completed_at = rs.getString("completed_at"),
            queuedAt = createdAt,
            queueDate = createdAt.take(10),
            queueTime = createdAt.drop(11).take(8)
        )
    }

    private fun mapArchivedTicket(rs: java.sql.ResultSet): ArchivedTicketModel {
        val waiting = rs.getLong("waiting_seconds").let { if (rs.wasNull()) null else it }
        val served = rs.getLong("served_seconds").let { if (rs.wasNull()) null else it }
        return ArchivedTicketModel(
            id = rs.getInt("id"),
            ticket_number = rs.getString("ticket_number"),
            department_id = rs.getInt("department_id"),
            queue_type_id = rs.getInt("queue_type_id"),
            status = rs.getString("status"),
            service_date = rs.getString("service_date"),
            queuedAt = rs.getString("created_at"),
            waitingSeconds = waiting,
            waitingDisplay = formatDurationToHms(waiting),
            servedSeconds = served,
            servedDisplay = formatDurationToHms(served)
        )
    }
}
