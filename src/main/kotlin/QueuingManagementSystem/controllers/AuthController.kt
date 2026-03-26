package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.GlobalCredentialResponse
import QueuingManagementSystem.models.LoginResponse
import QueuingManagementSystem.models.UserSessionModel
import QueuingManagementSystem.queries.getUserByTokenQuery
import QueuingManagementSystem.queries.getUserByUsernameAndPasswordQuery

class AuthController {
    fun login(username: String, password: String): LoginResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getUserByUsernameAndPasswordQuery).use { statement ->
                statement.setString(1, username)
                statement.setString(2, password)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return LoginResponse(
                            user_id = resultSet.getInt("id"),
                            full_name = resultSet.getString("full_name"),
                            role = resultSet.getString("role"),
                            department_id = resultSet.getInt("department_id")
                                .let { if (resultSet.wasNull()) null else it },
                            token = resultSet.getString("auth_token"),
                            result = GlobalCredentialResponse(200, true, "Login successful")
                        )
                    }
                }
            }
        }
        return LoginResponse(0, "", "", null, "", GlobalCredentialResponse(401, false, "Invalid credentials"))
    }

    fun getUserSessionByToken(token: String): UserSessionModel {
        if (token.isBlank()) return UserSessionModel()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getUserByTokenQuery).use { statement ->
                statement.setString(1, token)
                statement.executeQuery().use { resultSet ->
                    if (resultSet.next()) {
                        return UserSessionModel(
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
        return UserSessionModel()
    }
}
