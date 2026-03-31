package QueuingManagementSystem.services

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.common.formatDurationToHms
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*

class DisplayAggregationService {
    fun getDisplaySnapshot(displayBoardId: Int): DisplaySnapshotResponse {
        val display = getDisplayBoardById(displayBoardId)
        val windows = getDisplayWindows(displayBoardId)
        val queued = collectTickets(getQueuedTicketsForDisplayQuery, displayBoardId)
        val nowServing = collectTickets(getNowServingTicketsForDisplayQuery, displayBoardId)
        val skipped = collectTickets(getSkippedTicketsForDisplayQuery, displayBoardId)

        return DisplaySnapshotResponse(
            display = display,
            windows = windows,
            queued = queued,
            now_serving = nowServing,
            skipped = skipped,
            result = GlobalCredentialResponse(200, true, "OK")
        )
    }

    fun getDisplayAggregateSnapshot(displayBoardId: Int, filters: DisplayFilterParams): DisplayAggregateSnapshotResponse {
        val display = getDisplayBoardById(displayBoardId)
        val called = collectWindowState(getDisplayCurrentCalledTicketsQuery, displayBoardId, filters)
        val serving = collectWindowState(getDisplayCurrentServingTicketsQuery, displayBoardId, filters)
        val waitingCounts = collectWaitingCounts(displayBoardId, filters)
        val statusCounts = collectStatusCounts(displayBoardId, filters)
        val holdCount = statusCounts.firstOrNull { it.status == "HOLD" }?.count ?: 0
        val noShowCount = statusCounts.firstOrNull { it.status == "SKIPPED" }?.count ?: 0
        val visitorCount = collectVisitorCount(displayBoardId, filters)

        return DisplayAggregateSnapshotResponse(
            display = display,
            filters = filters,
            floor_filter_supported = false,
            current_called_tickets = called,
            current_serving_tickets = serving,
            waiting_counts_by_queue_type = waitingCounts,
            counts_by_status = statusCounts,
            hold_count = holdCount,
            no_show_count = noShowCount,
            visitor_count = visitorCount,
            special_count = null,
            result = GlobalCredentialResponse(200, true, "OK")
        )
    }

    private fun getDisplayBoardById(displayBoardId: Int): DisplayBoardModel? {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayBoardByIdQuery).use { s ->
                s.setInt(1, displayBoardId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        return DisplayBoardModel(
                            rs.getInt("id"),
                            rs.getInt("department_id"),
                            rs.getInt("area_id").let { if (rs.wasNull()) null else it },
                            rs.getString("code"),
                            rs.getString("name"),
                            rs.getBoolean("is_active")
                        )
                    }
                }
            }
        }
        return null
    }

    private fun getDisplayWindows(displayBoardId: Int): List<DisplayWindowModel> {
        val list = mutableListOf<DisplayWindowModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayBoardWindowsQuery).use { s ->
                s.setInt(1, displayBoardId)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            DisplayWindowModel(
                                rs.getInt("id"),
                                rs.getInt("department_id"),
                                rs.getInt("area_id").let { if (rs.wasNull()) null else it },
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun collectTickets(query: String, displayBoardId: Int): MutableList<DisplayTicketSnapshot> {
        val items = mutableListOf<DisplayTicketSnapshot>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(query).use { s ->
                s.setInt(1, displayBoardId)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        val waitingSeconds = rs.getLong("waiting_seconds").let { if (rs.wasNull()) null else it }
                        val servedSeconds = rs.getLong("served_seconds").let { if (rs.wasNull()) null else it }
                        items.add(
                            DisplayTicketSnapshot(
                                rs.getInt("id"),
                                rs.getString("ticket_number"),
                                rs.getInt("queue_type_id"),
                                rs.getString("queue_type_name"),
                                rs.getInt("assigned_window_id").let { if (rs.wasNull()) null else it },
                                rs.getString("assigned_window_name"),
                                rs.getString("status"),
                                rs.getString("created_at"),
                                rs.getString("queued_at"),
                                waitingSeconds,
                                formatDurationToHms(waitingSeconds),
                                servedSeconds,
                                formatDurationToHms(servedSeconds)
                            )
                        )
                    }
                }
            }
        }
        return items
    }

    private fun collectWindowState(query: String, displayBoardId: Int, filters: DisplayFilterParams): List<DisplayWindowTicketState> {
        val list = mutableListOf<DisplayWindowTicketState>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(query).use { s ->
                bindScopeParams(s, displayBoardId, filters)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            DisplayWindowTicketState(
                                window_id = rs.getInt("window_id"),
                                window_name = rs.getString("window_name"),
                                ticket_id = rs.getInt("id"),
                                ticket_number = rs.getString("ticket_number"),
                                queue_type_id = rs.getInt("queue_type_id"),
                                queue_type_name = rs.getString("queue_type_name"),
                                status = rs.getString("status")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun collectWaitingCounts(displayBoardId: Int, filters: DisplayFilterParams): List<QueueTypeWaitingCount> {
        val list = mutableListOf<QueueTypeWaitingCount>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayWaitingCountsByQueueTypeQuery).use { s ->
                bindScopeParams(s, displayBoardId, filters)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            QueueTypeWaitingCount(
                                queue_type_id = rs.getInt("queue_type_id"),
                                queue_type_name = rs.getString("queue_type_name"),
                                waiting_count = rs.getInt("waiting_count")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    private fun collectStatusCounts(displayBoardId: Int, filters: DisplayFilterParams): List<StatusCount> {
        val list = mutableListOf<StatusCount>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayCountsByStatusQuery).use { s ->
                bindScopeParams(s, displayBoardId, filters)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(StatusCount(rs.getString("status"), rs.getInt("count")))
                    }
                }
            }
        }
        return list
    }

    private fun collectVisitorCount(displayBoardId: Int, filters: DisplayFilterParams): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayVisitorCountQuery).use { s ->
                var idx = 1
                s.setInt(idx++, displayBoardId)
                bindNullableInt(s, idx++, filters.department_id)
                bindNullableInt(s, idx++, filters.department_id)
                bindNullableInt(s, idx++, filters.area_id)
                bindNullableInt(s, idx, filters.area_id)
                s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("visitor_count") }
            }
        }
        return 0
    }

    private fun bindScopeParams(statement: java.sql.PreparedStatement, displayBoardId: Int, filters: DisplayFilterParams) {
        var idx = 1
        statement.setInt(idx++, displayBoardId)
        bindNullableInt(statement, idx++, filters.department_id)
        bindNullableInt(statement, idx++, filters.department_id)
        bindNullableInt(statement, idx++, filters.area_id)
        bindNullableInt(statement, idx++, filters.area_id)
        bindNullableInt(statement, idx++, filters.company_id)
        bindNullableInt(statement, idx, filters.company_id)
    }

    private fun bindNullableInt(statement: java.sql.PreparedStatement, index: Int, value: Int?) {
        if (value == null) statement.setNull(index, java.sql.Types.INTEGER) else statement.setInt(index, value)
    }
}
