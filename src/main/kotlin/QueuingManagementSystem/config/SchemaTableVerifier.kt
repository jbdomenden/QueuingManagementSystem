package QueuingManagementSystem.config

import java.sql.Connection

object SchemaTableVerifier {
    private val createTableRegex = Regex("""(?i)CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+([a-zA-Z_][a-zA-Z0-9_]*)""")

    fun extractExpectedTables(schemaSql: String): Set<String> {
        return createTableRegex.findAll(schemaSql)
            .map { it.groupValues[1].lowercase() }
            .toSet()
    }

    fun verifyTablesExist(connection: Connection, schemaSql: String) {
        val expectedTables = extractExpectedTables(schemaSql)
        if (expectedTables.isEmpty()) {
            throw IllegalStateException("No CREATE TABLE statements found in schema.sql; cannot verify table mapping")
        }

        val actualTables = mutableSetOf<String>()
        connection.prepareStatement(
            """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 'public'
            """.trimIndent()
        ).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    actualTables.add(resultSet.getString("table_name").lowercase())
                }
            }
        }

        val missingTables = expectedTables.subtract(actualTables)
        if (missingTables.isNotEmpty()) {
            throw IllegalStateException(
                "Schema verification failed. Missing tables: ${missingTables.sorted().joinToString(", ")}"
            )
        }
    }
}
