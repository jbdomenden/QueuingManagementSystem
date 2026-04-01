package QueuingManagementSystem.config

import org.slf4j.LoggerFactory
import java.io.BufferedReader
import java.sql.Connection
import java.sql.SQLException

object LocalDatabaseInitializer {
    private val logger = LoggerFactory.getLogger(LocalDatabaseInitializer::class.java)
    private const val SCHEMA_RESOURCE = "schema.sql"

    fun initialize() {
        val appMode = (System.getenv("APP_MODE") ?: "local").uppercase()
        val authProvider = (System.getenv("AUTH_PROVIDER") ?: "local").uppercase()
        if (appMode != "LOCAL" || authProvider != "LOCAL") {
            logger.info("Skipping local schema initialization for APP_MODE={} AUTH_PROVIDER={}", appMode, authProvider)
            return
        }

        logger.info("Local schema initialization started")
        val sqlScript = loadSchemaSql()

        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                executeSqlScript(connection, sqlScript)
                connection.commit()
                logger.info("Local schema initialization completed")
            } catch (e: Exception) {
                connection.rollback()
                logger.error("Local schema initialization failed: {}", e.message, e)
                throw IllegalStateException(
                    "Local schema initialization failed using '$SCHEMA_RESOURCE': ${e.message}",
                    e
                )
            } finally {
                connection.autoCommit = true
            }
        }
    }

    private fun loadSchemaSql(): String {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(SCHEMA_RESOURCE)
            ?: throw IllegalStateException("Schema resource '$SCHEMA_RESOURCE' not found on classpath")
        return stream.bufferedReader().use(BufferedReader::readText)
    }

    private fun executeSqlScript(connection: Connection, script: String) {
        val statements = parseStatements(script)
        connection.createStatement().use { statement ->
            statements.forEach { sql ->
                try {
                    statement.execute(sql)
                } catch (e: SQLException) {
                    logger.error("Schema execution failed for SQL statement: {}", sql)
                    throw IllegalStateException("Failed SQL statement: $sql", e)
                }
            }
        }
    }

    private fun parseStatements(script: String): List<String> {
        val statements = mutableListOf<String>()
        val current = StringBuilder()
        var inSingleQuote = false
        var inDoubleQuote = false

        for (ch in script) {
            when (ch) {
                '\'' -> if (!inDoubleQuote) inSingleQuote = !inSingleQuote
                '"' -> if (!inSingleQuote) inDoubleQuote = !inDoubleQuote
                ';' -> {
                    if (!inSingleQuote && !inDoubleQuote) {
                        val sql = current.toString().trim()
                        if (sql.isNotBlank()) statements.add(sql)
                        current.clear()
                        continue
                    }
                }
            }
            current.append(ch)
        }

        val trailing = current.toString().trim()
        if (trailing.isNotBlank()) statements.add(trailing)
        return statements
    }
}
