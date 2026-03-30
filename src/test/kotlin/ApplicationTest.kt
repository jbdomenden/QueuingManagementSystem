package QueuingManagementSystem

import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertTrue

class ApplicationTest {
    @Test
    fun testModuleStarts() = testApplication {
        application { module() }
        assertTrue(true)
    }
}
