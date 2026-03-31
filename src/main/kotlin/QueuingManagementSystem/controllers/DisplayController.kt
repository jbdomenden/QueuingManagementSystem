package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*
import QueuingManagementSystem.services.DisplayAggregationService

class DisplayController {
    private val aggregationService = DisplayAggregationService()
    fun createDisplayBoard(request: DisplayBoardRequest): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(postDisplayBoardQuery).use { s ->
                s.setInt(1, request.department_id)
                if (request.area_id == null) s.setNull(2, java.sql.Types.INTEGER) else s.setInt(2, request.area_id)
                s.setString(3, request.code)
                s.setString(4, request.name)
                s.setBoolean(5, request.is_active)
                s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateDisplayBoard(request: DisplayBoardRequest): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(updateDisplayBoardQuery).use { s ->
                if (request.area_id == null) s.setNull(1, java.sql.Types.INTEGER) else s.setInt(1, request.area_id)
                s.setString(2, request.code)
                s.setString(3, request.name)
                s.setBoolean(4, request.is_active)
                s.setInt(5, request.id ?: 0)
                return s.executeUpdate() > 0
            }
        }
    }

    fun getDisplayBoards(): MutableList<DisplayBoardModel> {
        val list = mutableListOf<DisplayBoardModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayBoardsQuery).use { s ->
                s.executeQuery().use { rs ->
                    while (rs.next()) list.add(mapDisplay(rs))
                }
            }
        }
        return list
    }

    fun assignWindows(request: DisplayBoardWindowAssignmentRequest): Boolean {
        ConnectionPoolManager.getConnection().use { c ->
            c.autoCommit = false
            try {
                c.prepareStatement(deleteDisplayBoardWindowsQuery).use { s ->
                    s.setInt(1, request.display_board_id)
                    s.executeUpdate()
                }
                c.prepareStatement(postDisplayBoardWindowQuery).use { s ->
                    request.window_ids.forEach { wid ->
                        s.setInt(1, request.display_board_id)
                        s.setInt(2, wid)
                        s.addBatch()
                    }
                    s.executeBatch()
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

    fun getDisplaySnapshot(displayBoardId: Int): DisplaySnapshotResponse {
        return aggregationService.getDisplaySnapshot(displayBoardId)
    }

    fun getDisplayAggregateSnapshot(displayBoardId: Int, filters: DisplayFilterParams): DisplayAggregateSnapshotResponse {
        return aggregationService.getDisplayAggregateSnapshot(displayBoardId, filters)
    }


    fun getDisplayWallboard(displayBoardId: Int, selectedFilter: String?): DisplayWallboardResponse {
        return aggregationService.getDisplayWallboard(displayBoardId, selectedFilter)
    }

    fun getDisplayBoardById(displayBoardId: Int): DisplayBoardModel? {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDisplayBoardByIdQuery).use { s ->
                s.setInt(1, displayBoardId)
                s.executeQuery().use { rs -> if (rs.next()) return mapDisplay(rs) }
            }
        }
        return null
    }

    fun getDisplayWindows(displayBoardId: Int): MutableList<DisplayWindowModel> {
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

    private fun mapDisplay(rs: java.sql.ResultSet): DisplayBoardModel {
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
