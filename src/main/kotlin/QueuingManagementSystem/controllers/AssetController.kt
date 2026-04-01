package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.AssetModel
import QueuingManagementSystem.models.AssetRequest
import QueuingManagementSystem.queries.*
import java.security.SecureRandom

class AssetController {
    fun listAssets(): List<AssetModel> {
        val items = mutableListOf<AssetModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getAssetsQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) items.add(rs.toAssetModel())
                }
            }
        }
        return items
    }

    fun getAssetById(id: Int): AssetModel? = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(getAssetByIdQuery).use { statement ->
            statement.setInt(1, id)
            statement.executeQuery().use { rs -> if (rs.next()) rs.toAssetModel() else null }
        }
    }

    fun createAsset(request: AssetRequest): Int = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(createAssetQuery).use { statement ->
            val key = normalizeDeviceKey(request.assetType, request.deviceKey)
            bindAsset(statement, request, key)
            statement.executeQuery().use { rs -> if (rs.next()) rs.getInt("id") else 0 }
        }
    }

    fun updateAsset(id: Int, request: AssetRequest): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(updateAssetQuery).use { statement ->
            val key = normalizeDeviceKey(request.assetType, request.deviceKey)
            bindAsset(statement, request, key)
            statement.setInt(12, id)
            statement.executeUpdate() > 0
        }
    }

    fun updateStatus(id: Int, status: String): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(updateAssetStatusQuery).use { statement ->
            statement.setString(1, status)
            statement.setInt(2, id)
            statement.executeUpdate() > 0
        }
    }

    fun deleteAsset(id: Int): Boolean = ConnectionPoolManager.getConnection().use { connection ->
        connection.prepareStatement(deleteAssetQuery).use { statement ->
            statement.setInt(1, id)
            statement.executeUpdate() > 0
        }
    }

    fun regenerateDeviceKey(id: Int): String {
        val key = generateDeviceKey()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement("UPDATE assets SET device_key = ?, updated_at = NOW() WHERE id = ?").use { s ->
                s.setString(1, key)
                s.setInt(2, id)
                s.executeUpdate()
            }
        }
        return key
    }

    fun isValidType(type: String) = setOf("KIOSK", "DISPLAY").contains(type)
    fun isValidStatus(status: String) = setOf("ACTIVE", "INACTIVE", "MAINTENANCE", "RETIRED").contains(status)

    private fun normalizeDeviceKey(type: String, input: String?): String? {
        if (!isValidType(type)) return input
        return input?.takeIf { it.isNotBlank() } ?: generateDeviceKey()
    }

    private fun bindAsset(statement: java.sql.PreparedStatement, request: AssetRequest, deviceKey: String?) {
        statement.setString(1, request.assetTag)
        statement.setString(2, request.assetName)
        statement.setString(3, request.assetType)
        if (deviceKey == null) statement.setNull(4, java.sql.Types.VARCHAR) else statement.setString(4, deviceKey)
        if (request.ipAddress.isNullOrBlank()) statement.setNull(5, java.sql.Types.VARCHAR) else statement.setString(5, request.ipAddress)
        if (request.macAddress.isNullOrBlank()) statement.setNull(6, java.sql.Types.VARCHAR) else statement.setString(6, request.macAddress)
        statement.setString(7, request.status)
        if (request.assignedDepartmentId == null) statement.setNull(8, java.sql.Types.INTEGER) else statement.setInt(8, request.assignedDepartmentId)
        if (request.assignedCompanyId == null) statement.setNull(9, java.sql.Types.INTEGER) else statement.setInt(9, request.assignedCompanyId)
        if (request.location.isNullOrBlank()) statement.setNull(10, java.sql.Types.VARCHAR) else statement.setString(10, request.location)
        if (request.notes.isNullOrBlank()) statement.setNull(11, java.sql.Types.VARCHAR) else statement.setString(11, request.notes)
    }

    private fun java.sql.ResultSet.toAssetModel() = AssetModel(
        id = getInt("id"),
        assetTag = getString("asset_tag"),
        assetName = getString("asset_name"),
        assetType = getString("asset_type"),
        deviceKey = getString("device_key"),
        ipAddress = getString("ip_address"),
        macAddress = getString("mac_address"),
        status = getString("status"),
        assignedDepartmentId = getInt("assigned_department_id").let { if (wasNull()) null else it },
        assignedCompanyId = getInt("assigned_company_id").let { if (wasNull()) null else it },
        location = getString("location"),
        notes = getString("notes")
    )

    private fun generateDeviceKey(length: Int = 48): String {
        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val random = SecureRandom()
        return (1..length).joinToString("") { chars[random.nextInt(chars.length)].toString() }
    }
}
