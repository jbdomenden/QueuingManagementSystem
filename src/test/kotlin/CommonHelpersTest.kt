package QueuingManagementSystem

import QueuingManagementSystem.common.formatDurationToHms
import kotlin.test.Test
import kotlin.test.assertEquals

class CommonHelpersTest {
    @Test
    fun formatDurationToHmsHandlesNullAndValues() {
        assertEquals("--:--:--", formatDurationToHms(null))
        assertEquals("00:00:59", formatDurationToHms(59))
        assertEquals("01:01:01", formatDurationToHms(3661))
    }
}
