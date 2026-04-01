package QueuingManagementSystem.config

import java.io.BufferedReader

object DatabaseSchemaInitializer {
    private const val SCHEMA_RESOURCE = "schema.sql"

    fun initialize() {
        val sqlScript = loadSchemaSql()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                executeSqlScript(connection, sqlScript)
                connection.commit()
            } catch (e: Exception) {
                connection.rollback()
                throw IllegalStateException("Database schema initialization failed using '$SCHEMA_RESOURCE': ${e.message}", e)
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

    private fun executeSqlScript(connection: java.sql.Connection, script: String) {
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

        connection.createStatement().use { statement ->
            statements.forEach { sql -> statement.execute(sql) }
        }
    }
}
