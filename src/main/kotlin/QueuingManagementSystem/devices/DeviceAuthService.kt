package QueuingManagementSystem.devices

import QueuingManagementSystem.config.ConnectionPoolManager

class DeviceAuthService {
    fun authenticateDevice(deviceKey: String, expectedType: DeviceType? = null): DeviceContext? {
        if (deviceKey.isBlank()) return null

        val sql = """
            SELECT id, device_name, device_type, company_id, department_id
            FROM queue_devices
            WHERE device_key = ?
              AND is_active = true
            LIMIT 1
        """.trimIndent()

        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, deviceKey)
                statement.executeQuery().use { rs ->
                    if (!rs.next()) return null
                    val type = runCatching { DeviceType.valueOf(rs.getString("device_type")) }.getOrNull() ?: return null
                    if (expectedType != null && expectedType != type) return null

                    val context = DeviceContext(
                        deviceId = rs.getInt("id"),
                        deviceName = rs.getString("device_name"),
                        deviceType = type,
                        companyId = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
                        departmentId = rs.getInt("department_id").let { if (rs.wasNull()) null else it }
                    )
                    connection.prepareStatement("UPDATE queue_devices SET last_seen_at = NOW(), updated_at = NOW() WHERE id = ?").use { up ->
                        up.setInt(1, context.deviceId)
                        up.executeUpdate()
                    }
                    return context
                }
            }
        }
    }

    fun resolveDeviceContext(deviceKey: String): DeviceContext? = authenticateDevice(deviceKey, null)
}
