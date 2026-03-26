package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.AreaModel
import QueuingManagementSystem.models.AreaRequest
import QueuingManagementSystem.queries.getAreasByDepartmentQuery
import QueuingManagementSystem.queries.postAreaQuery
import QueuingManagementSystem.queries.updateAreaQuery

class AreaController {
    fun createArea(request: AreaRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postAreaQuery).use { statement ->
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

    fun updateArea(request: AreaRequest): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateAreaQuery).use { statement ->
                statement.setString(1, request.name)
                statement.setBoolean(2, request.is_active)
                statement.setInt(3, request.id ?: 0)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun getAreasByDepartment(departmentId: Int): MutableList<AreaModel> {
        val list = mutableListOf<AreaModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getAreasByDepartmentQuery).use { statement ->
                statement.setInt(1, departmentId)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            AreaModel(
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
}
