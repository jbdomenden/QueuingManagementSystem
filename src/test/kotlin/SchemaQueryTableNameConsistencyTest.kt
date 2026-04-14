package QueuingManagementSystem

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.name
import kotlin.streams.asSequence
import kotlin.test.Test
import kotlin.test.assertTrue

class SchemaQueryTableNameConsistencyTest {
    private val createTableRegex = Regex("""(?i)CREATE\s+TABLE\s+IF\s+NOT\s+EXISTS\s+([a-zA-Z_][a-zA-Z0-9_]*)""")
    private val cteRegex = Regex("""(?i)(?:WITH|,)\s*([a-zA-Z_][a-zA-Z0-9_]*)\s+AS\s*\(""")
    private val tableReferenceRegex = Regex("""(?i)\b(?:FROM|JOIN|INTO|DELETE\s+FROM)\s+([a-zA-Z_][a-zA-Z0-9_]*)\b""")

    @Test
    fun queryTableNamesMapToSchemaTables() {
        val schemaSql = Files.readString(Path.of("src/main/resources/schema.sql"))
        val schemaTables = createTableRegex.findAll(schemaSql)
            .map { it.groupValues[1].lowercase() }
            .toSet()

        val queriesDir = Path.of("src/main/kotlin/QueuingManagementSystem/queries")
        val missingByFile = linkedMapOf<String, MutableSet<String>>()

        Files.walk(queriesDir).use { stream ->
            stream.asSequence()
                .filter { it.toString().endsWith(".kt") }
                .sortedBy { it.name }
                .forEach { file ->
                    val source = Files.readString(file)
                    val ctes = cteRegex.findAll(source).map { it.groupValues[1].lowercase() }.toSet()
                    tableReferenceRegex.findAll(source).forEach { match ->
                        val table = match.groupValues[1].lowercase()
                        if (table !in ctes && table !in schemaTables) {
                            missingByFile.getOrPut(file.toString()) { linkedSetOf() }.add(table)
                        }
                    }
                }
        }

        assertTrue(
            missingByFile.isEmpty(),
            "Found table names referenced in queries but missing from schema.sql: $missingByFile"
        )
    }
}
