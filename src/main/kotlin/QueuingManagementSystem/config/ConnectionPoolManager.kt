package QueuingManagementSystem.config

import java.sql.Connection
import java.sql.DriverManager

object ConnectionPoolManager {
    private var jdbcUrl: String = "jdbc:postgresql://localhost:5433/qms_db"
    private var jdbcUser: String = "postgres"
    private var jdbcPassword: String = "postgres"

    fun configure(url: String, user: String, password: String) {
        jdbcUrl = url
        jdbcUser = user
        jdbcPassword = password
    }

    fun getConnection(): Connection {
        Class.forName("org.postgresql.Driver")
        return DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)
    }
}
