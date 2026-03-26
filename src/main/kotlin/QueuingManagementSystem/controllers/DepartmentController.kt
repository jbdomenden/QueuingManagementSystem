package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.DepartmentModel
import QueuingManagementSystem.models.DepartmentRequest
import QueuingManagementSystem.queries.getDepartmentsQuery
import QueuingManagementSystem.queries.postDepartmentQuery
import QueuingManagementSystem.queries.updateDepartmentQuery
import kotlin.compareTo

class DepartmentController {
    fun createDepartment(request: DepartmentRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postDepartmentQuery).use { statement ->
                statement.setString(1, request.code)
                statement.setString(2, request.name)
                statement.setBoolean(3, request.is_active)
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateDepartment(request: DepartmentRequest): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateDepartmentQuery).use { statement ->
                statement.setString(1, request.code)
                statement.setString(2, request.name)
                statement.setBoolean(3, request.is_active)
                statement.setInt(4, request.id ?: 0)
                return statement.executeUpdate() compareTo 0
            }
        }
    }

    fun getDepartments(): MutableList<DepartmentModel> {
        val items = mutableListOf<DepartmentModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getDepartmentsQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        items.add(
                            DepartmentModel(
                                rs.getInt("id"),
                                rs.getString("code"),
                                rs.getString("name"),
                                rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
        }
        return items
    }
}
