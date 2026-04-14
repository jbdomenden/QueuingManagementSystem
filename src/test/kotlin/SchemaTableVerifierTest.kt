package QueuingManagementSystem

import QueuingManagementSystem.config.SchemaTableVerifier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SchemaTableVerifierTest {
    @Test
    fun extractExpectedTablesFindsAllCreateTableEntries() {
        val schema = """
            CREATE TABLE IF NOT EXISTS departments (id SERIAL PRIMARY KEY);
            CREATE TABLE IF NOT EXISTS users (id SERIAL PRIMARY KEY);
            CREATE TABLE IF NOT EXISTS user_sessions (id SERIAL PRIMARY KEY);
        """.trimIndent()

        val tables = SchemaTableVerifier.extractExpectedTables(schema)

        assertEquals(setOf("departments", "users", "user_sessions"), tables)
    }

    @Test
    fun extractExpectedTablesFailsOnEmptySchemaDuringVerification() {
        val dataSource = org.h2.jdbcx.JdbcDataSource().apply {
            setURL("jdbc:h2:mem:test_schema_verifier;MODE=PostgreSQL;DATABASE_TO_UPPER=false")
            user = "sa"
            password = ""
        }

        dataSource.connection.use { connection ->
            assertFailsWith<IllegalStateException> {
                SchemaTableVerifier.verifyTablesExist(connection, "SELECT 1;")
            }
        }
    }
}
