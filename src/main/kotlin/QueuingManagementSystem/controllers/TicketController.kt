package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.TicketCreateRequest
import QueuingManagementSystem.models.TicketModel
import marlow.systems.queuingsystem.queries.*

class TicketController {
    fun createTicket(request: QueuingManagementSystem.models.TicketCreateRequest): QueuingManagementSystem.models.TicketModel {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var departmentId = 0
                var prefix = ""
                connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getQueueTypeWithDepartmentByIdQuery).use { statement ->
                    statement.setInt(1, request.queue_type_id)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            departmentId = rs.getInt("department_id")
                            prefix = rs.getString("prefix")
                        }
                    }
                }
                if (departmentId == 0) throw IllegalStateException("queue type not found")

                var sequence = 0
                connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.upsertQueueDailySequenceQuery).use { statement ->
                    statement.setInt(1, request.queue_type_id)
                    statement.executeQuery().use { rs -> if (rs.next()) sequence = rs.getInt("current_value") }
                }

                val ticketNumber = "$prefix-${sequence.toString().padStart(3, '0')}"
                var ticketId = 0
                connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postTicketQuery).use { statement ->
                    statement.setString(1, ticketNumber)
                    statement.setInt(2, departmentId)
                    statement.setInt(3, request.queue_type_id)
                    statement.setInt(4, request.kiosk_id)
                    statement.executeQuery().use { rs -> if (rs.next()) ticketId = rs.getInt("id") }
                }

                connection.commit()
                return _root_ide_package_.QueuingManagementSystem.models.TicketModel(
                    ticketId,
                    ticketNumber,
                    departmentId,
                    request.queue_type_id,
                    request.kiosk_id,
                    null,
                    null,
                    "WAITING",
                    ""
                )
            } catch (e: Exception) {
                connection.rollback()
                return _root_ide_package_.QueuingManagementSystem.models.TicketModel(
                    0,
                    "",
                    0,
                    0,
                    null,
                    null,
                    null,
                    "",
                    ""
                )
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getLiveTickets(departmentId: Int): MutableList<QueuingManagementSystem.models.TicketModel> {
        val list = mutableListOf<QueuingManagementSystem.models.TicketModel>()
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getLiveTicketsByDepartmentQuery).use { statement ->
                statement.setInt(1, departmentId)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            _root_ide_package_.QueuingManagementSystem.models.TicketModel(
                                id = rs.getInt("id"),
                                ticket_number = rs.getString("ticket_number"),
                                department_id = rs.getInt("department_id"),
                                queue_type_id = rs.getInt("queue_type_id"),
                                kiosk_id = rs.getInt("kiosk_id").let { if (rs.wasNull()) null else it },
                                assigned_window_id = rs.getInt("assigned_window_id")
                                    .let { if (rs.wasNull()) null else it },
                                assigned_handler_id = rs.getInt("assigned_handler_id")
                                    .let { if (rs.wasNull()) null else it },
                                status = rs.getString("status"),
                                created_at = rs.getString("created_at"),
                                called_at = rs.getString("called_at"),
                                completed_at = rs.getString("completed_at")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun callNext(handlerId: Int): QueuingManagementSystem.models.TicketModel {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getWaitingTicketForHandlerCallNextWithLockingQuery).use { statement ->
                    statement.setInt(1, handlerId)
                    statement.setInt(2, handlerId)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            val model = _root_ide_package_.QueuingManagementSystem.models.TicketModel(
                                rs.getInt("id"),
                                rs.getString("ticket_number"),
                                rs.getInt("department_id"),
                                rs.getInt("queue_type_id"),
                                rs.getInt("kiosk_id").let { if (rs.wasNull()) null else it },
                                rs.getInt("assigned_window_id").let { if (rs.wasNull()) null else it },
                                rs.getInt("assigned_handler_id").let { if (rs.wasNull()) null else it },
                                rs.getString("status"),
                                rs.getString("created_at"),
                                rs.getString("called_at"),
                                rs.getString("completed_at")
                            )
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
        return _root_ide_package_.QueuingManagementSystem.models.TicketModel(0, "", 0, 0, null, null, null, "", "")
    }

    fun updateTicketStatus(ticketId: Int, handlerId: Int, action: String): Boolean {
        val query = when (action) {
            "IN_SERVICE" -> _root_ide_package_.QueuingManagementSystem.queries.updateTicketToInServiceQuery
            "SKIPPED" -> _root_ide_package_.QueuingManagementSystem.queries.updateTicketToSkippedQuery
            "CALLED" -> _root_ide_package_.QueuingManagementSystem.queries.updateTicketToCalledRecallQuery
            "COMPLETED" -> _root_ide_package_.QueuingManagementSystem.queries.updateTicketToCompletedQuery
            else -> ""
        }
        if (query.isBlank()) return false
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(query).use { statement ->
                statement.setInt(1, ticketId)
                statement.setInt(2, handlerId)
                return statement.executeUpdate() > 0
            }
        }
    }
}
