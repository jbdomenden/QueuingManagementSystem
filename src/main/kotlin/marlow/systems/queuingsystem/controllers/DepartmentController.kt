package marlow.systems.queuingsystem.controllers

import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.DepartmentModel
import marlow.systems.queuingsystem.models.DepartmentRequest
import marlow.systems.queuingsystem.queries.getDepartmentsQuery
import marlow.systems.queuingsystem.queries.postDepartmentQuery
import marlow.systems.queuingsystem.queries.updateDepartmentQuery

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
                return statement.executeUpdate() > 0
            }
        }
    }

    fun getDepartments(): MutableList<DepartmentModel> {
        val items = mutableListOf<DepartmentModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getDepartmentsQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        items.add(DepartmentModel(rs.getInt("id"), rs.getString("code"), rs.getString("name"), rs.getBoolean("is_active")))
                    }
                }
            }
        }
        return items
    }
}
