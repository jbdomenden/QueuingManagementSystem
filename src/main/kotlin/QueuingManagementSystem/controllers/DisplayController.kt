package QueuingManagementSystem.controllers

import QueuingManagementSystem.models.DisplayBoardModel
import QueuingManagementSystem.models.DisplayBoardRequest
import QueuingManagementSystem.models.DisplayBoardWindowAssignmentRequest
import QueuingManagementSystem.models.DisplaySnapshotResponse
import QueuingManagementSystem.models.DisplayTicketSnapshot
import QueuingManagementSystem.queries.deleteDisplayBoardWindowsQuery
import QueuingManagementSystem.queries.getDisplayBoardsQuery
import QueuingManagementSystem.queries.postDisplayBoardQuery
import QueuingManagementSystem.queries.postDisplayBoardWindowQuery
import QueuingManagementSystem.queries.updateDisplayBoardQuery
import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.*
import marlow.systems.queuingsystem.queries.*
import java.sql.Types

class DisplayController {
    fun createDisplayBoard(request: DisplayBoardRequest): Int { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        postDisplayBoardQuery
    ).use { s -> s.setInt(1, request.department_id); if (request.area_id == null) s.setNull(2, Types.INTEGER) else s.setInt(2, request.area_id); s.setString(3, request.code); s.setString(4, request.name); s.setBoolean(5, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateDisplayBoard(request: DisplayBoardRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        updateDisplayBoardQuery
    ).use { s -> if (request.area_id == null) s.setNull(1, Types.INTEGER) else s.setInt(1, request.area_id); s.setString(2, request.code); s.setString(3, request.name); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getDisplayBoards(): MutableList<DisplayBoardModel> { val list = mutableListOf<DisplayBoardModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getDisplayBoardsQuery
    ).use { s -> s.executeQuery().use { rs -> while (rs.next()) list.add(
        DisplayBoardModel(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getInt("area_id").let { if (rs.wasNull()) null else it },
            rs.getString("code"),
            rs.getString("name"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun assignWindows(request: DisplayBoardWindowAssignmentRequest): Boolean { ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        deleteDisplayBoardWindowsQuery
    ).use { s -> s.setInt(1, request.display_board_id); s.executeUpdate() }; c.prepareStatement(
        postDisplayBoardWindowQuery
    ).use { s -> request.window_ids.forEach { wid -> s.setInt(1, request.display_board_id); s.setInt(2, wid); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
    fun getDisplaySnapshot(displayBoardId: Int): DisplaySnapshotResponse {
        fun collect(query: String): MutableList<DisplayTicketSnapshot> { val items = mutableListOf<DisplayTicketSnapshot>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(query).use { s -> s.setInt(1, displayBoardId); s.executeQuery().use { rs -> while (rs.next()) items.add(
            DisplayTicketSnapshot(
                rs.getInt("id"),
                rs.getString("ticket_number"),
                rs.getInt("queue_type_id"),
                rs.getString("queue_type_name"),
                rs.getInt("assigned_window_id").let { if (rs.wasNull()) null else it },
                rs.getString("assigned_window_name"),
                rs.getString("status"),
                rs.getString("created_at")
            )
        ) } } } }; return items }
        return DisplaySnapshotResponse(collect(getQueuedTicketsForDisplayQuery), collect(getNowServingTicketsForDisplayQuery), collect(getSkippedTicketsForDisplayQuery), GlobalCredentialResponse(200, true, "OK"))
    }
}
