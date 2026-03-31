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
                var resolvedQueueTypeId = request.queue_type_id ?: 0
                var resolvedCompanyId = request.company_id
                var resolvedCompanyTransactionId = request.company_transaction_id
                var resolvedDestinationId = request.destination_id
                val resolvedTransactionFamily = request.transaction_family?.trim()?.ifBlank { null }

                if (resolvedDestinationId != null) {
                    var destinationQueueTypeId = 0
                    var destinationCompanyId = 0
                    var destinationCompanyTransactionId = 0
                    var destinationStatus = ""
                    var destinationTransactionStatus = ""
                    var destinationCompanyStatus = ""
                    connection.prepareStatement(getDestinationDetailsByIdQuery).use { statement ->
                        statement.setInt(1, resolvedDestinationId)
                        statement.executeQuery().use { rs ->
                            if (rs.next()) {
                                destinationQueueTypeId = rs.getInt("queue_type_id").let { if (rs.wasNull()) 0 else it }
                                destinationCompanyId = rs.getInt("company_id")
                                destinationCompanyTransactionId = rs.getInt("company_transaction_id")
                                destinationStatus = rs.getString("status")
                                destinationTransactionStatus = rs.getString("transaction_status")
                                destinationCompanyStatus = rs.getString("company_status")
                            }
                        }
                    }
                    if (destinationQueueTypeId <= 0) throw IllegalStateException("destination queue type is not configured")
                    if (destinationStatus != "ACTIVE" || destinationTransactionStatus != "ACTIVE" || destinationCompanyStatus != "ACTIVE") throw IllegalStateException("destination is inactive")
                    resolvedQueueTypeId = destinationQueueTypeId
                    if (resolvedCompanyId == null) resolvedCompanyId = destinationCompanyId
                    if (resolvedCompanyTransactionId == null) resolvedCompanyTransactionId = destinationCompanyTransactionId
                }
                if (resolvedQueueTypeId <= 0) throw IllegalStateException("queue type not found")

                var departmentId = 0
                var prefix = ""
                var queueTypeCompanyId: Int? = null
                connection.prepareStatement(getQueueTypeWithDepartmentByIdQuery).use { statement ->
                    statement.setInt(1, resolvedQueueTypeId)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) {
                            departmentId = rs.getInt("department_id")
                            prefix = rs.getString("prefix")
                            queueTypeCompanyId = rs.getInt("company_id").let { if (rs.wasNull()) null else it }
                        }
                    }
                }
                if (departmentId == 0) throw IllegalStateException("queue type not found")
                if (resolvedCompanyId != null && queueTypeCompanyId != resolvedCompanyId) throw IllegalStateException("queue type does not belong to selected company")

                if (resolvedCompanyId != null && resolvedCompanyTransactionId != null) {
                    var companyStatus = ""
                    connection.prepareStatement(getActiveCompanyByIdQuery).use { statement ->
                        statement.setInt(1, resolvedCompanyId)
                        statement.executeQuery().use { rs -> if (rs.next()) companyStatus = rs.getString("status") }
                    }
                    if (companyStatus != "ACTIVE") throw IllegalStateException("company is inactive or not found")

                    var ctCompanyId = 0
                    var ctStatus = ""
                    var ctCompanyStatus = ""
                    connection.prepareStatement(getCompanyTransactionDetailsByIdQuery).use { statement ->
                        statement.setInt(1, resolvedCompanyTransactionId)
                        statement.executeQuery().use { rs ->
                            if (rs.next()) {
                                ctCompanyId = rs.getInt("company_id")
                                ctStatus = rs.getString("status")
                                ctCompanyStatus = rs.getString("company_status")
                            }
                        }
                    }
                    if (ctCompanyId <= 0 || ctCompanyId != resolvedCompanyId) throw IllegalStateException("invalid company transaction")
                    if (ctStatus != "ACTIVE" || ctCompanyStatus != "ACTIVE") throw IllegalStateException("company transaction is inactive")
                }

                var kioskAllowed = false
                connection.prepareStatement(getKioskByIdAndDepartmentQueueTypeQuery).use { statement ->
                    statement.setInt(1, request.kiosk_id)
                    statement.setInt(2, resolvedQueueTypeId)
                    statement.executeQuery().use { rs -> kioskAllowed = rs.next() }
                }
                if (!kioskAllowed) throw IllegalStateException("kiosk is not mapped to queue type")

                var resolvedWorkflowTemplateId: Int? = null
                connection.prepareStatement(getActiveWorkflowTemplateByBindingQuery).use { statement ->
                    statement.setInt(1, departmentId)
                    statement.setInt(2, resolvedQueueTypeId)
                    if (resolvedCompanyId == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, resolvedCompanyId)
                    if (resolvedCompanyTransactionId == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, resolvedCompanyTransactionId)
                    statement.setString(5, resolvedTransactionFamily)
                    statement.executeQuery().use { rs ->
                        if (rs.next()) resolvedWorkflowTemplateId = rs.getInt("id").let { if (rs.wasNull()) null else it }
                    }
                }

                var sequence = 0
                connection.prepareStatement(upsertQueueDailySequenceQuery).use { statement ->
                    statement.setInt(1, resolvedQueueTypeId)
                    statement.executeQuery().use { rs -> if (rs.next()) sequence = rs.getInt("current_value") }
                }

                val ticketNumber = "$prefix-${sequence.toString().padStart(3, '0')}"
                var ticket = TicketModel(
                    id = 0,
                    ticket_number = "",
                    department_id = 0,
                    queue_type_id = 0,
                    company_transaction_id = null,
                    kiosk_id = null,
                    assigned_window_id = null,
                    assigned_handler_id = null,
                    status = "",
                    created_at = ""
                )
                connection.prepareStatement(postTicketQuery).use { statement ->
                    statement.setString(1, ticketNumber)
                    statement.setInt(2, departmentId)
                    statement.setInt(3, resolvedQueueTypeId)
                    if (resolvedCompanyId == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, resolvedCompanyId)
                    if (resolvedCompanyTransactionId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, resolvedCompanyTransactionId)
                    if (resolvedDestinationId == null) statement.setNull(6, java.sql.Types.INTEGER) else statement.setInt(6, resolvedDestinationId)
                    statement.setString(7, request.crew_identifier)
                    statement.setString(8, request.crew_identifier_type)
                    statement.setString(9, request.crew_name)
                    statement.setString(10, resolvedTransactionFamily)
                    if (resolvedWorkflowTemplateId == null) statement.setNull(11, java.sql.Types.INTEGER) else statement.setInt(11, resolvedWorkflowTemplateId)
                    statement.setInt(12, request.kiosk_id)
                    statement.executeQuery().use { rs -> if (rs.next()) ticket = mapTicket(rs) }
                }

                if (ticket.id <= 0) throw IllegalStateException("failed to insert ticket")

                connection.prepareStatement(postTicketLogQuery).use { statement ->
                    statement.setInt(1, ticket.id)
                    statement.setString(2, "CREATED")
                    statement.setNull(3, java.sql.Types.INTEGER)
                    statement.setString(4, "{\"kiosk_id\":${request.kiosk_id},\"company_id\":${resolvedCompanyId},\"company_transaction_id\":${resolvedCompanyTransactionId},\"destination_id\":${resolvedDestinationId},\"transaction_family\":\"${resolvedTransactionFamily ?: ""}\",\"workflow_template_id\":${resolvedWorkflowTemplateId},\"crew_identifier\":\"${request.crew_identifier ?: ""}\"}")
                    statement.executeUpdate()
                }

                connection.commit()
                return ticket
            } catch (e: Exception) {
                connection.rollback()
                return TicketModel(
                    id = 0,
                    ticket_number = "",
                    department_id = 0,
                    queue_type_id = 0,
                    company_transaction_id = null,
                    kiosk_id = null,
                    assigned_window_id = null,
                    assigned_handler_id = null,
                    status = "",
                    created_at = ""
                )
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
                PrintableTicketModel(0, "", 0, "", null, null, null, 0, "", "", "", "", "", ""),
                GlobalCredentialResponse(400, false, "Ticket create failed")
            )
        }

        val printable = getPrintableTicketDetails(ticket.id)
            ?: PrintableTicketModel(
                ticket.id,
                ticket.ticket_number,
                ticket.department_id,
                "",
                null,
                null,
                null,
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
                            val companyName = rs.getString("company_name")
                            if (!companyName.isNullOrBlank()) appendLine("Company: $companyName")
                            val transactionName = rs.getString("company_transaction_name")
                            if (!transactionName.isNullOrBlank()) appendLine("Transaction: $transactionName")
                            val destinationName = rs.getString("destination_name")
                            if (!destinationName.isNullOrBlank()) appendLine("Destination: $destinationName")
                            appendLine("Queue Type: ${rs.getString("queue_type_name")}")
                            appendLine("Date: ${rs.getString("queue_date")}")
                            append("Time: ${rs.getString("queue_time")}\nPlease wait for your number to be called")
                        }
                        return PrintableTicketModel(
                            ticketId = rs.getInt("ticket_id"),
                            ticketNumber = rs.getString("ticket_number"),
                            departmentId = rs.getInt("department_id"),
                            departmentName = rs.getString("department_name"),
                            companyName = rs.getString("company_name"),
                            companyTransactionName = rs.getString("company_transaction_name"),
                            destinationName = rs.getString("destination_name"),
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
                val processReference = "EOD_${serviceDateParam}_${System.currentTimeMillis()}"
                val metadata = "{\"service_date\":\"$serviceDateParam\",\"department_id\":${departmentIdParam ?: "null"}}"
                connection.prepareStatement(upsertDailyQueueArchiveByServiceDateQuery).use { statement ->
                    statement.setString(1, processReference)
                    statement.setString(2, metadata)
                    statement.setInt(3, actorUserIdParam)
                    statement.setString(4, serviceDateParam)
                    if (departmentIdParam == null) {
                        statement.setNull(5, java.sql.Types.INTEGER)
                        statement.setNull(6, java.sql.Types.INTEGER)
                    } else {
                        statement.setInt(5, departmentIdParam)
                        statement.setInt(6, departmentIdParam)
                    }
                    statement.executeUpdate()
                }
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
                insertAudit(connection, actorUserIdParam, departmentIdParam, "QUEUE_DAILY_ARCHIVE", "daily_queue_archive", serviceDateParam, "archived_count=$affected")
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
        return TicketModel(
            id = 0,
            ticket_number = "",
            department_id = 0,
            queue_type_id = 0,
            company_transaction_id = null,
            kiosk_id = null,
            assigned_window_id = null,
            assigned_handler_id = null,
            status = "",
            created_at = ""
        )
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


    data class LifecycleMutationResult(
        val response: TicketLifecycleResponse,
        val displayIds: List<Int> = emptyList(),
        val departmentId: Int? = null
    )

    fun getHandlerActiveContext(handlerId: Int): HandlerActiveContextResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getHandlerWindowContextQuery).use { statement ->
                statement.setInt(1, handlerId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        val activeTicket = getCurrentActiveTicket(handlerId)
                        return HandlerActiveContextResponse(
                            handler_id = rs.getInt("handler_id"),
                            user_id = rs.getInt("user_id"),
                            department_id = rs.getInt("department_id"),
                            window_id = rs.getInt("window_id"),
                            active_ticket = activeTicket,
                            result = GlobalCredentialResponse(200, true, "OK")
                        )
                    }
                }
            }
        }
        return HandlerActiveContextResponse(0, 0, 0, 0, null, GlobalCredentialResponse(404, false, "No active handler context"))
    }

    fun getHandlerDashboardMetrics(handlerId: Int): HandlerDashboardMetrics {
        val ctx = getHandlerActiveContext(handlerId)
        if (!ctx.result.Access) return HandlerDashboardMetrics(0, 0, 0, 0, 0, 0, 0)
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getHandlerDashboardMetricsQuery).use { statement ->
                statement.setInt(1, ctx.department_id)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        return HandlerDashboardMetrics(
                            waiting_count = rs.getInt("waiting_count"),
                            called_count = rs.getInt("called_count"),
                            serving_count = rs.getInt("serving_count"),
                            hold_count = rs.getInt("hold_count"),
                            no_show_count = rs.getInt("no_show_count"),
                            completed_count = rs.getInt("completed_count"),
                            cancelled_count = rs.getInt("cancelled_count")
                        )
                    }
                }
            }
        }
        return HandlerDashboardMetrics(0, 0, 0, 0, 0, 0, 0)
    }

    fun getCurrentActiveTicket(handlerId: Int): TicketModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCurrentHandlerActiveTicketQuery).use { statement ->
                statement.setInt(1, handlerId)
                statement.executeQuery().use { rs -> if (rs.next()) return mapTicket(rs) }
            }
        }
        return null
    }

    fun callNextTicket(actorUserId: Int, handlerId: Int): LifecycleMutationResult {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val existing = fetchCurrentActiveTicketForUpdate(connection, handlerId)
                if (existing != null) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(existing, "CALL_NEXT", GlobalCredentialResponse(409, false, "Handler already has an active ticket")))
                }

                val ticket = fetchOldestWaitingForUpdate(connection, handlerId)
                    ?: run {
                        connection.rollback()
                        return LifecycleMutationResult(TicketLifecycleResponse(null, "CALL_NEXT", GlobalCredentialResponse(404, false, "No waiting ticket available")))
                    }

                applyStatusTransition(
                    connection = connection,
                    actorUserId = actorUserId,
                    actorHandlerId = handlerId,
                    ticket = ticket,
                    toStatus = "CALLED",
                    reason = "CALL_NEXT",
                    event = "TICKET_CALLED",
                    targetHandlerId = handlerId,
                    targetWindowId = getActiveWindowId(connection, handlerId)
                )

                val updated = fetchTicketForUpdate(connection, ticket.id) ?: ticket
                val displayIds = getDisplayIdsByHandler(handlerId)
                connection.commit()
                return LifecycleMutationResult(TicketLifecycleResponse(updated, "TICKET_CALLED", GlobalCredentialResponse(200, true, "Ticket called")), displayIds, updated.department_id)
            } catch (e: Exception) {
                connection.rollback()
                return LifecycleMutationResult(TicketLifecycleResponse(null, "CALL_NEXT", GlobalCredentialResponse(500, false, e.message ?: "Internal server error")))
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun recallTicket(actorUserId: Int, handlerId: Int, ticketId: Int, reason: String?): LifecycleMutationResult {
        return transitionCurrentTicket(actorUserId, handlerId, ticketId, setOf("CALLED", "IN_SERVICE", "HOLD"), "CALLED", reason ?: "RECALL", "TICKET_RECALLED")
    }

    fun holdTicket(actorUserId: Int, handlerId: Int, ticketId: Int, reason: String?): LifecycleMutationResult {
        return transitionCurrentTicket(actorUserId, handlerId, ticketId, setOf("CALLED", "IN_SERVICE"), "HOLD", reason ?: "HOLD", "TICKET_HOLD")
    }

    fun noShowTicket(actorUserId: Int, handlerId: Int, ticketId: Int, reason: String?): LifecycleMutationResult {
        return transitionCurrentTicket(actorUserId, handlerId, ticketId, setOf("CALLED", "HOLD"), "SKIPPED", reason ?: "NO_SHOW", "TICKET_NO_SHOW")
    }

    fun completeTicket(actorUserId: Int, handlerId: Int, ticketId: Int, reason: String?): LifecycleMutationResult {
        return transitionCurrentTicket(actorUserId, handlerId, ticketId, setOf("IN_SERVICE", "CALLED"), "COMPLETED", reason ?: "COMPLETED", "TICKET_COMPLETED")
    }

    fun cancelTicket(actorUserId: Int, handlerId: Int?, ticketId: Int, reason: String): LifecycleMutationResult {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val ticket = fetchTicketForUpdate(connection, ticketId)
                    ?: return LifecycleMutationResult(TicketLifecycleResponse(null, "TICKET_CANCELLED", GlobalCredentialResponse(404, false, "Ticket not found")))

                if (ticket.status in setOf("COMPLETED", "CANCELLED", "TRANSFERRED")) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(ticket, "TICKET_CANCELLED", GlobalCredentialResponse(409, false, "Illegal transition from ${ticket.status} to CANCELLED")))
                }

                applyStatusTransition(connection, actorUserId, handlerId, ticket, "CANCELLED", reason, "TICKET_CANCELLED")
                val updated = fetchTicketForUpdate(connection, ticketId) ?: ticket
                val displayIds = if (updated.assigned_handler_id != null) getDisplayIdsByHandler(updated.assigned_handler_id) else emptyList()
                connection.commit()
                return LifecycleMutationResult(TicketLifecycleResponse(updated, "TICKET_CANCELLED", GlobalCredentialResponse(200, true, "Ticket cancelled")), displayIds, updated.department_id)
            } catch (e: Exception) {
                connection.rollback()
                return LifecycleMutationResult(TicketLifecycleResponse(null, "TICKET_CANCELLED", GlobalCredentialResponse(500, false, e.message ?: "Internal server error")))
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun transferTicket(actorUserId: Int, request: TicketTransferRequest): LifecycleMutationResult {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val ticket = fetchTicketForUpdate(connection, request.ticket_id)
                    ?: return LifecycleMutationResult(TicketLifecycleResponse(null, "TICKET_TRANSFERRED", GlobalCredentialResponse(404, false, "Ticket not found")))

                if (ticket.assigned_handler_id != request.handler_id) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(ticket, "TICKET_TRANSFERRED", GlobalCredentialResponse(403, false, "Ticket is not assigned to handler")))
                }

                if (ticket.status !in setOf("CALLED", "IN_SERVICE", "HOLD")) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(ticket, "TICKET_TRANSFERRED", GlobalCredentialResponse(409, false, "Illegal transition from ${ticket.status} to TRANSFERRED")))
                }

                val targetDepartmentId = request.target_department_id ?: ticket.department_id
                val targetQueueTypeId = request.target_queue_type_id ?: ticket.queue_type_id
                val targetWindowId = request.target_window_id
                val targetCompanyTransactionId = request.target_company_transaction_id ?: ticket.company_transaction_id

                connection.prepareStatement(updateTicketLifecycleByIdQuery).use { statement ->
                    statement.setString(1, "TRANSFERRED")
                    statement.setNull(2, java.sql.Types.INTEGER)
                    statement.setNull(3, java.sql.Types.INTEGER)
                    statement.setInt(4, targetQueueTypeId)
                    statement.setInt(5, targetDepartmentId)
                    if (targetCompanyTransactionId == null) statement.setNull(6, java.sql.Types.INTEGER) else statement.setInt(6, targetCompanyTransactionId)
                    statement.setString(7, "TRANSFERRED")
                    statement.setString(8, "TRANSFERRED")
                    statement.setString(9, "TRANSFERRED")
                    statement.setInt(10, ticket.id)
                    statement.executeUpdate()
                }

                insertStatusHistory(connection, ticket, "TRANSFERRED", actorUserId, request.handler_id, request.reason, "{\"transfer\":true}")
                insertTicketLog(connection, ticket.id, "TRANSFERRED", request.handler_id, request.reason)
                insertAudit(connection, actorUserId, ticket.department_id, "TICKET_TRANSFERRED", "ticket", ticket.id.toString(), request.reason)
                connection.prepareStatement(insertTicketTransferQuery).use { statement ->
                    statement.setInt(1, ticket.id)
                    statement.setInt(2, ticket.queue_type_id)
                    statement.setInt(3, targetQueueTypeId)
                    statement.setInt(4, ticket.department_id)
                    statement.setInt(5, targetDepartmentId)
                    if (ticket.assigned_window_id == null) statement.setNull(6, java.sql.Types.INTEGER) else statement.setInt(6, ticket.assigned_window_id)
                    if (targetWindowId == null) statement.setNull(7, java.sql.Types.INTEGER) else statement.setInt(7, targetWindowId)
                    if (ticket.company_transaction_id == null) statement.setNull(8, java.sql.Types.INTEGER) else statement.setInt(8, ticket.company_transaction_id)
                    if (targetCompanyTransactionId == null) statement.setNull(9, java.sql.Types.INTEGER) else statement.setInt(9, targetCompanyTransactionId)
                    statement.setInt(10, actorUserId)
                    statement.setInt(11, request.handler_id)
                    statement.setString(12, request.reason)
                    statement.executeUpdate()
                }

                val updated = fetchTicketForUpdate(connection, ticket.id) ?: ticket
                val displayIds = getDisplayIdsByHandler(request.handler_id)
                connection.commit()
                return LifecycleMutationResult(TicketLifecycleResponse(updated, "TICKET_TRANSFERRED", GlobalCredentialResponse(200, true, "Ticket transferred")), displayIds, ticket.department_id)
            } catch (e: Exception) {
                connection.rollback()
                return LifecycleMutationResult(TicketLifecycleResponse(null, "TICKET_TRANSFERRED", GlobalCredentialResponse(500, false, e.message ?: "Internal server error")))
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun getUserTicketHistory(handlerId: Int, limit: Int, offset: Int): List<TicketModel> {
        val list = mutableListOf<TicketModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getUserTicketHistoryQuery).use { statement ->
                statement.setInt(1, handlerId)
                statement.setInt(2, limit)
                statement.setInt(3, offset)
                statement.executeQuery().use { rs -> while (rs.next()) list.add(mapTicket(rs)) }
            }
        }
        return list
    }

    private fun transitionCurrentTicket(actorUserId: Int, handlerId: Int, ticketId: Int, allowedFrom: Set<String>, toStatus: String, reason: String, event: String): LifecycleMutationResult {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val ticket = fetchTicketForUpdate(connection, ticketId)
                    ?: return LifecycleMutationResult(TicketLifecycleResponse(null, event, GlobalCredentialResponse(404, false, "Ticket not found")))

                if (ticket.assigned_handler_id != handlerId) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(ticket, event, GlobalCredentialResponse(403, false, "Ticket is not assigned to handler")))
                }

                if (!allowedFrom.contains(ticket.status)) {
                    connection.rollback()
                    return LifecycleMutationResult(TicketLifecycleResponse(ticket, event, GlobalCredentialResponse(409, false, "Illegal transition from ${ticket.status} to $toStatus")))
                }

                applyStatusTransition(connection, actorUserId, handlerId, ticket, toStatus, reason, event)
                val updated = fetchTicketForUpdate(connection, ticket.id) ?: ticket
                val displayIds = getDisplayIdsByHandler(handlerId)
                connection.commit()
                return LifecycleMutationResult(TicketLifecycleResponse(updated, event, GlobalCredentialResponse(200, true, "Ticket updated")), displayIds, updated.department_id)
            } catch (e: Exception) {
                connection.rollback()
                return LifecycleMutationResult(TicketLifecycleResponse(null, event, GlobalCredentialResponse(500, false, e.message ?: "Internal server error")))
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun applyStatusTransition(connection: java.sql.Connection, actorUserId: Int, actorHandlerId: Int?, ticket: TicketModel, toStatus: String, reason: String, event: String, targetHandlerId: Int? = null, targetWindowId: Int? = null) {
        connection.prepareStatement(updateTicketLifecycleByIdQuery).use { statement ->
            statement.setString(1, toStatus)
            if (targetHandlerId == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, targetHandlerId)
            if (targetWindowId == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, targetWindowId)
            statement.setNull(4, java.sql.Types.INTEGER)
            statement.setNull(5, java.sql.Types.INTEGER)
            statement.setNull(6, java.sql.Types.INTEGER)
            statement.setString(7, toStatus)
            statement.setString(8, toStatus)
            statement.setString(9, toStatus)
            statement.setInt(10, ticket.id)
            statement.executeUpdate()
        }

        if (targetHandlerId != null || targetWindowId != null) {
            connection.prepareStatement(insertTicketAssignmentHistoryQuery).use { statement ->
                statement.setInt(1, ticket.id)
                if (ticket.assigned_handler_id == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, ticket.assigned_handler_id)
                statement.setInt(3, targetHandlerId ?: ticket.assigned_handler_id ?: 0)
                if (ticket.assigned_window_id == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, ticket.assigned_window_id)
                if (targetWindowId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, targetWindowId)
                statement.setInt(6, actorUserId)
                if (actorHandlerId == null) statement.setNull(7, java.sql.Types.INTEGER) else statement.setInt(7, actorHandlerId)
                statement.setString(8, reason)
                statement.executeUpdate()
            }
        }

        insertStatusHistory(connection, ticket, toStatus, actorUserId, actorHandlerId, reason, "{\"event\":\"$event\"}")
        insertTicketLog(connection, ticket.id, toStatus, actorHandlerId, reason)
        insertAudit(connection, actorUserId, ticket.department_id, event, "ticket", ticket.id.toString(), reason)
    }

    private fun insertStatusHistory(connection: java.sql.Connection, ticket: TicketModel, toStatus: String, actorUserId: Int, actorHandlerId: Int?, reason: String, metadataJson: String) {
        connection.prepareStatement(insertQueueStatusHistoryQuery).use { statement ->
            statement.setInt(1, ticket.id)
            statement.setString(2, ticket.status)
            statement.setString(3, toStatus)
            statement.setInt(4, actorUserId)
            if (actorHandlerId == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, actorHandlerId)
            statement.setString(6, reason)
            statement.setString(7, metadataJson)
            statement.executeUpdate()
        }
    }

    private fun insertTicketLog(connection: java.sql.Connection, ticketId: Int, action: String, actorHandlerId: Int?, reason: String) {
        connection.prepareStatement(postTicketLogQuery).use { statement ->
            statement.setInt(1, ticketId)
            statement.setString(2, action)
            if (actorHandlerId == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, actorHandlerId)
            statement.setString(4, "{\"reason\":\"${reason.replace("\"", "'")}\"}")
            statement.executeUpdate()
        }
    }

    private fun insertAudit(connection: java.sql.Connection, actorUserId: Int, departmentId: Int?, action: String, entityName: String, entityId: String, reason: String) {
        connection.prepareStatement(postSessionLifecycleAuditQuery).use { statement ->
            statement.setInt(1, actorUserId)
            if (departmentId == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, departmentId)
            statement.setString(3, action)
            statement.setString(4, entityName)
            statement.setString(5, entityId)
            statement.setString(6, "{\"reason\":\"${reason.replace("\"", "'")}\"}")
            statement.executeUpdate()
        }
    }

    private fun fetchTicketForUpdate(connection: java.sql.Connection, ticketId: Int): TicketModel? {
        connection.prepareStatement(getTicketForUpdateQuery).use { statement ->
            statement.setInt(1, ticketId)
            statement.executeQuery().use { rs -> if (rs.next()) return mapTicket(rs) }
        }
        return null
    }

    private fun fetchCurrentActiveTicketForUpdate(connection: java.sql.Connection, handlerId: Int): TicketModel? {
        connection.prepareStatement(getCurrentHandlerActiveTicketQuery).use { statement ->
            statement.setInt(1, handlerId)
            statement.executeQuery().use { rs -> if (rs.next()) return mapTicket(rs) }
        }
        return null
    }

    private fun fetchOldestWaitingForUpdate(connection: java.sql.Connection, handlerId: Int): TicketModel? {
        connection.prepareStatement(getOldestWaitingTicketForHandlerQuery).use { statement ->
            statement.setInt(1, handlerId)
            statement.executeQuery().use { rs -> if (rs.next()) return mapTicket(rs) }
        }
        return null
    }

    private fun getActiveWindowId(connection: java.sql.Connection, handlerId: Int): Int? {
        connection.prepareStatement(getActiveHandlerWindowQuery).use { statement ->
            statement.setInt(1, handlerId)
            statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("window_id") }
        }
        return null
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
            company_id = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
            company_transaction_id = rs.getInt("company_transaction_id").let { if (rs.wasNull()) null else it },
            destination_id = rs.getInt("destination_id").let { if (rs.wasNull()) null else it },
            crew_identifier = rs.getString("crew_identifier"),
            crew_identifier_type = rs.getString("crew_identifier_type"),
            crew_name = rs.getString("crew_name"),
            transaction_family = rs.getString("transaction_family"),
            workflow_template_id = rs.getInt("workflow_template_id").let { if (rs.wasNull()) null else it },
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
