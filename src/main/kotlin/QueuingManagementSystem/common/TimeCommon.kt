package QueuingManagementSystem.common

fun formatDurationToHms(seconds: Long?): String {
    if (seconds == null || seconds < 0) return "--:--:--"
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}
