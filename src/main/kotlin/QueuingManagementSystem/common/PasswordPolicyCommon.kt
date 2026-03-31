package QueuingManagementSystem.common

object PasswordPolicy {
    private val upper = Regex("[A-Z]")
    private val lower = Regex("[a-z]")
    private val digit = Regex("\\d")
    private val symbol = Regex("[^A-Za-z0-9]")

    fun validate(password: String, fieldName: String): String? {
        if (password.length < 12) return "$fieldName must be at least 12 characters"
        if (!upper.containsMatchIn(password)) return "$fieldName must include at least one uppercase letter"
        if (!lower.containsMatchIn(password)) return "$fieldName must include at least one lowercase letter"
        if (!digit.containsMatchIn(password)) return "$fieldName must include at least one number"
        if (!symbol.containsMatchIn(password)) return "$fieldName must include at least one symbol"
        return null
    }
}
