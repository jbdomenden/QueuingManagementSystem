package QueuingManagementSystem.controllers

import java.util.UUID
import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.UserModel
import QueuingManagementSystem.models.UserRequest
import QueuingManagementSystem.queries.getUsersByDepartmentQuery
import QueuingManagementSystem.queries.getUsersQuery
import QueuingManagementSystem.queries.postUserQuery
import QueuingManagementSystem.queries.updateUserQuery

class UserController {
    fun createUser(request: QueuingManagementSystem.models.UserRequest): Int {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.postUserQuery).use { statement ->
                statement.setString(1, request.username)
                statement.setString(2, request.password ?: "")
                statement.setString(3, request.full_name)
                statement.setString(4, request.role)
                if (request.department_id == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, request.department_id)
                statement.setBoolean(6, request.is_active)
                statement.setString(7, UUID.randomUUID().toString())
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateUser(request: QueuingManagementSystem.models.UserRequest): Boolean {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.updateUserQuery).use { statement ->
                statement.setString(1, request.full_name)
                statement.setString(2, request.role)
                if (request.department_id == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, request.department_id)
                statement.setBoolean(4, request.is_active)
                statement.setInt(5, request.id ?: 0)
                return statement.executeUpdate() > 0
            }
        }
    }

    fun getUsers(departmentId: Int?): MutableList<QueuingManagementSystem.models.UserModel> {
        val list = mutableListOf<QueuingManagementSystem.models.UserModel>()
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            val query = if (departmentId == null) _root_ide_package_.QueuingManagementSystem.queries.getUsersQuery else _root_ide_package_.QueuingManagementSystem.queries.getUsersByDepartmentQuery
            connection.prepareStatement(query).use { statement ->
                if (departmentId != null) statement.setInt(1, departmentId)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            _root_ide_package_.QueuingManagementSystem.models.UserModel(
                                rs.getInt("id"),
                                rs.getString("username"),
                                rs.getString("full_name"),
                                rs.getString("role"),
                                rs.getInt("department_id").let { if (rs.wasNull()) null else it },
                                rs.getBoolean("is_active")
                            )
                        )
                    }
                }
            }
        }
        return list
    }
}
