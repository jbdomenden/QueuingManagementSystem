package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.DepartmentModel
import QueuingManagementSystem.models.DepartmentRequest
import QueuingManagementSystem.queries.getDepartmentsQuery
import QueuingManagementSystem.queries.postDepartmentQuery
import QueuingManagementSystem.queries.updateDepartmentQuery

class DepartmentController {
    fun createDepartment(request: QueuingManagementSystem.models.DepartmentRequest): Int {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postDepartmentQuery).use { statement ->
                statement.setString(1, request.code)
                statement.setString(2, request.name)
                statement.setBoolean(3, request.is_active)
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateDepartment(request: QueuingManagementSystem.models.DepartmentRequest): Boolean {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.updateDepartmentQuery).use { statement ->
                statement.setString(1, request.code)
                statement.setString(2, request.name)
                statement.setBoolean(3, request.is_active)
                statement.setInt(4, request.id ?: 0)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun getDepartments(): MutableList<QueuingManagementSystem.models.DepartmentModel> {
        val items = mutableListOf<QueuingManagementSystem.models.DepartmentModel>()
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getDepartmentsQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        items.add(
                            _root_ide_package_.QueuingManagementSystem.models.DepartmentModel(
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
