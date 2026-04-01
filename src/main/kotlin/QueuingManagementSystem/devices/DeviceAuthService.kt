package QueuingManagementSystem.devices

import QueuingManagementSystem.config.ConnectionPoolManager

class DeviceAuthService {
    fun authenticateDevice(deviceKey: String, expectedType: DeviceType? = null): DeviceContext? {
        if (deviceKey.isBlank()) return null

        val sql = """
            SELECT id, asset_name, asset_type, assigned_company_id, assigned_department_id
            FROM assets
            WHERE device_key = ?
              AND status = 'ACTIVE'
            LIMIT 1
        """.trimIndent()

        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, deviceKey)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val type = runCatching { DeviceType.valueOf(rs.getString("asset_type")) }.getOrNull() ?: return null
                    if (expectedType != null && expectedType != type) return null

                    return DeviceContext(
                        deviceId = rs.getInt("id"),
                        deviceName = rs.getString("asset_name"),
                        deviceType = type,
                        companyId = rs.getInt("assigned_company_id").let { if (rs.wasNull()) null else it },
                        departmentId = rs.getInt("assigned_department_id").let { if (rs.wasNull()) null else it }
                    )
                }
            }
        }
    }

    fun resolveDeviceContext(deviceKey: String): DeviceContext? = authenticateDevice(deviceKey, null)
}
