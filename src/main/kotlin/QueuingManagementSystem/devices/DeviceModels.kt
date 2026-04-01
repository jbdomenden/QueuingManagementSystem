package QueuingManagementSystem.devices

enum class DeviceType {
    KIOSK,
    DISPLAY
}

data class DeviceContext(
    val deviceId: Int,
    val deviceName: String,
    val deviceType: DeviceType,
    val companyId: Int?,
    val departmentId: Int?
)
