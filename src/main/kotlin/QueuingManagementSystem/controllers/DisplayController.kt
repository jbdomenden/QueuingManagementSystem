package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.*
import marlow.systems.queuingsystem.queries.*

class DisplayController {
    fun createDisplayBoard(request: QueuingManagementSystem.models.DisplayBoardRequest): Int { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.postDisplayBoardQuery
    ).use { s -> s.setInt(1, request.department_id); if (request.area_id == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, request.area_id); s.setString(3, request.code); s.setString(4, request.name); s.setBoolean(5, request.is_active); s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") } } }; return 0 }
    fun updateDisplayBoard(request: QueuingManagementSystem.models.DisplayBoardRequest): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.updateDisplayBoardQuery
    ).use { s -> if (request.area_id == null) s.setNull(1, java.sql.Types.INTEGER) else s.setInt(1, request.area_id); s.setString(2, request.code); s.setString(3, request.name); s.setBoolean(4, request.is_active); s.setInt(5, request.id ?: 0); return s.executeUpdate() > 0 } } }
    fun getDisplayBoards(): MutableList<QueuingManagementSystem.models.DisplayBoardModel> { val list = mutableListOf<QueuingManagementSystem.models.DisplayBoardModel>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getDisplayBoardsQuery
    ).use { s -> s.executeQuery().use { rs -> while (rs.next()) list.add(
        _root_ide_package_.QueuingManagementSystem.models.DisplayBoardModel(
            rs.getInt("id"),
            rs.getInt("department_id"),
            rs.getInt("area_id").let { if (rs.wasNull()) null else it },
            rs.getString("code"),
            rs.getString("name"),
            rs.getBoolean("is_active")
        )
    ) } } }; return list }
    fun assignWindows(request: QueuingManagementSystem.models.DisplayBoardWindowAssignmentRequest): Boolean { _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.autoCommit = false; try { c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.deleteDisplayBoardWindowsQuery
    ).use { s -> s.setInt(1, request.display_board_id); s.executeUpdate() }; c.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postDisplayBoardWindowQuery).use { s -> request.window_ids.forEach { wid -> s.setInt(1, request.display_board_id); s.setInt(2, wid); s.addBatch() }; s.executeBatch() }; c.commit(); return true } catch (e: Exception) { c.rollback(); return false } finally { c.autoCommit = true } } }
    fun getDisplaySnapshot(displayBoardId: Int): QueuingManagementSystem.models.DisplaySnapshotResponse {
        fun collect(query: String): MutableList<QueuingManagementSystem.models.DisplayTicketSnapshot> { val items = mutableListOf<QueuingManagementSystem.models.DisplayTicketSnapshot>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(query).use { s -> s.setInt(1, displayBoardId); s.executeQuery().use { rs -> while (rs.next()) items.add(
            _root_ide_package_.QueuingManagementSystem.models.DisplayTicketSnapshot(
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
