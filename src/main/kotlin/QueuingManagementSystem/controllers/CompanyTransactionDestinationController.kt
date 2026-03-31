package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.CompanyTransactionDestination
import QueuingManagementSystem.models.CompanyTransactionDestinationKioskItem
import QueuingManagementSystem.models.CompanyTransactionDestinationRequest
import QueuingManagementSystem.queries.*

class CompanyTransactionDestinationController {
    fun getCompanyTransactionDestinationsByTransactionId(companyTransactionIdParam: Int): List<CompanyTransactionDestination> {
        val list = mutableListOf<CompanyTransactionDestination>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getCompanyTransactionDestinationsByTransactionIdQuery).use { s ->
                s.setInt(1, companyTransactionIdParam)
                s.executeQuery().use { rs -> while (rs.next()) list.add(mapDestination(rs)) }
            }
        }
        return list
    }

    fun getActiveCompanyTransactionDestinationsForKiosk(companyTransactionIdParam: Int): List<CompanyTransactionDestinationKioskItem> {
        val list = mutableListOf<CompanyTransactionDestinationKioskItem>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getActiveCompanyTransactionDestinationsForKioskQuery).use { s ->
                s.setInt(1, companyTransactionIdParam)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            CompanyTransactionDestinationKioskItem(
                                id = rs.getInt("id"),
                                companyTransactionId = rs.getInt("company_transaction_id"),
                                destinationCode = rs.getString("destination_code"),
                                destinationName = rs.getString("destination_name"),
                                destinationSubtitle = rs.getString("destination_subtitle"),
                                queueTypeId = rs.getInt("queue_type_id").let { if (rs.wasNull()) null else it },
                                sortOrder = rs.getInt("sort_order")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun getCompanyTransactionDestinationById(idParam: Int): CompanyTransactionDestination {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getCompanyTransactionDestinationByIdQuery).use { s ->
                s.setInt(1, idParam)
                s.executeQuery().use { rs -> if (rs.next()) return mapDestination(rs) }
            }
        }
        return CompanyTransactionDestination(0, 0, "", "", null, null, 0, "INACTIVE", "", "")
    }

    fun postCompanyTransactionDestination(request: CompanyTransactionDestinationRequest): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(postCompanyTransactionDestinationQuery).use { s ->
                s.setInt(1, request.companyTransactionId)
                s.setString(2, request.destinationCode)
                s.setString(3, request.destinationName)
                s.setString(4, request.destinationSubtitle)
                if (request.queueTypeId == null) s.setNull(5, java.sql.Types.INTEGER) else s.setInt(5, request.queueTypeId)
                s.setInt(6, request.sortOrder)
                s.setString(7, request.status)
                s.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateCompanyTransactionDestination(idParam: Int, request: CompanyTransactionDestinationRequest): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(updateCompanyTransactionDestinationQuery).use { s ->
                s.setInt(1, request.companyTransactionId)
                s.setString(2, request.destinationCode)
                s.setString(3, request.destinationName)
                s.setString(4, request.destinationSubtitle)
                if (request.queueTypeId == null) s.setNull(5, java.sql.Types.INTEGER) else s.setInt(5, request.queueTypeId)
                s.setInt(6, request.sortOrder)
                s.setString(7, request.status)
                s.setInt(8, idParam)
                return s.executeUpdate()
            }
        }
    }

    fun toggleCompanyTransactionDestinationStatus(idParam: Int, statusParam: String): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(toggleCompanyTransactionDestinationStatusQuery).use { s ->
                s.setString(1, statusParam)
                s.setInt(2, idParam)
                return s.executeUpdate()
            }
        }
    }

    fun deactivateCompanyTransactionDestination(idParam: Int): Int {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(deactivateCompanyTransactionDestinationQuery).use { s ->
                s.setInt(1, idParam)
                return s.executeUpdate()
            }
        }
    }

    private fun mapDestination(rs: java.sql.ResultSet): CompanyTransactionDestination {
        return CompanyTransactionDestination(
            id = rs.getInt("id"),
            companyTransactionId = rs.getInt("company_transaction_id"),
            destinationCode = rs.getString("destination_code"),
            destinationName = rs.getString("destination_name"),
            destinationSubtitle = rs.getString("destination_subtitle"),
            queueTypeId = rs.getInt("queue_type_id").let { if (rs.wasNull()) null else it },
            sortOrder = rs.getInt("sort_order"),
            status = rs.getString("status"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }
}
