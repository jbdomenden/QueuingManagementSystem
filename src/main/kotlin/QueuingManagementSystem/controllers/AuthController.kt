package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.LoginResponse
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.UserSessionModel
import QueuingManagementSystem.queries.getUserByTokenQuery
import QueuingManagementSystem.queries.getUserByUsernameAndPasswordQuery

class AuthController {
    fun login(username: String, password: String): QueuingManagementSystem.models.LoginResponse {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getUserByUsernameAndPasswordQuery).use { statement ->
                statement.setString(1, username)
                statement.setString(2, password)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return _root_ide_package_.QueuingManagementSystem.models.LoginResponse(
                            user_id = resultSet.getInt("id"),
                            full_name = resultSet.getString("full_name"),
                            role = resultSet.getString("role"),
                            department_id = resultSet.getInt("department_id")
                                .let { if (resultSet.wasNull()) null else it },
                            token = resultSet.getString("auth_token"),
                            result = _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                                200,
                                true,
                                "Login successful"
                            )
                        )
                    }
                }
            }
        }
        return _root_ide_package_.QueuingManagementSystem.models.LoginResponse(
            0,
            "",
            "",
            null,
            "",
            _root_ide_package_.QueuingManagementSystem.models.GlobalCredentialResponse(
                401,
                false,
                "Invalid credentials"
            )
        )
    }

    fun getUserSessionByToken(token: String): QueuingManagementSystem.models.UserSessionModel {
        if (token.isBlank()) return _root_ide_package_.QueuingManagementSystem.models.UserSessionModel()
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(_root_ide_package_.QueuingManagementSystem.queries.getUserByTokenQuery).use { statement ->
                statement.setString(1, token)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return _root_ide_package_.QueuingManagementSystem.models.UserSessionModel(
                            user_id = resultSet.getInt("id"),
                            department_id = resultSet.getInt("department_id")
                                .let { if (resultSet.wasNull()) null else it },
                            role = resultSet.getString("role"),
                            token = resultSet.getString("auth_token")
                        )
                    }
                }
            }
        }
        return _root_ide_package_.QueuingManagementSystem.models.UserSessionModel()
    }
}
