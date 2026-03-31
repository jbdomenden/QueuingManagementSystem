package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.CompanyTransaction
import QueuingManagementSystem.models.CompanyTransactionKioskItem
import QueuingManagementSystem.models.CompanyTransactionRequest
import QueuingManagementSystem.queries.*

class CompanyTransactionController {
    fun getCompanyTransactionsByCompanyId(companyIdParam: Int): List<CompanyTransaction> {
        val list = mutableListOf<CompanyTransaction>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompanyTransactionsByCompanyIdQuery).use { statement ->
                statement.setInt(1, companyIdParam)
                statement.executeQuery().use { rs -> while (rs.next()) list.add(mapCompanyTransaction(rs)) }
            }
        }
        return list
    }

    fun getActiveCompanyTransactionsForKiosk(companyIdParam: Int): List<CompanyTransactionKioskItem> {
        val list = mutableListOf<CompanyTransactionKioskItem>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveCompanyTransactionsForKioskQuery).use { statement ->
                statement.setInt(1, companyIdParam)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            CompanyTransactionKioskItem(
                                id = rs.getInt("id"),
                                companyId = rs.getInt("company_id"),
                                transactionCode = rs.getString("transaction_code"),
                                transactionName = rs.getString("transaction_name"),
                                transactionSubtitle = rs.getString("transaction_subtitle"),
                                requiresCrewValidation = rs.getBoolean("requires_crew_validation"),
                                inputMode = rs.getString("input_mode"),
                                sortOrder = rs.getInt("sort_order")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun getCompanyTransactionById(companyTransactionIdParam: Int): CompanyTransaction {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompanyTransactionByIdQuery).use { statement ->
                statement.setInt(1, companyTransactionIdParam)
                statement.executeQuery().use { rs -> if (rs.next()) return mapCompanyTransaction(rs) }
            }
        }
        return CompanyTransaction(0, 0, "", "", null, 0, "INACTIVE", "", "")
    }

    fun postCompanyTransaction(request: CompanyTransactionRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postCompanyTransactionQuery).use { statement ->
                statement.setInt(1, request.companyId)
                statement.setString(2, request.transactionCode)
                statement.setString(3, request.transactionName)
                statement.setString(4, request.transactionSubtitle)
                statement.setBoolean(5, request.requiresCrewValidation)
                statement.setString(6, request.inputMode)
                statement.setInt(7, request.sortOrder)
                statement.setString(8, request.status)
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateCompanyTransaction(companyTransactionIdParam: Int, request: CompanyTransactionRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateCompanyTransactionQuery).use { statement ->
                statement.setInt(1, request.companyId)
                statement.setString(2, request.transactionCode)
                statement.setString(3, request.transactionName)
                statement.setString(4, request.transactionSubtitle)
                statement.setBoolean(5, request.requiresCrewValidation)
                statement.setString(6, request.inputMode)
                statement.setInt(7, request.sortOrder)
                statement.setString(8, request.status)
                statement.setInt(9, companyTransactionIdParam)
                return statement.executeUpdate()
            }
        }
    }

    fun deactivateCompanyTransaction(companyTransactionIdParam: Int): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(deactivateCompanyTransactionQuery).use { statement ->
                statement.setInt(1, companyTransactionIdParam)
                return statement.executeUpdate()
            }
        }
    }

    fun toggleCompanyTransactionStatus(companyTransactionIdParam: Int, statusParam: String): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(toggleCompanyTransactionStatusQuery).use { statement ->
                statement.setString(1, statusParam)
                statement.setInt(2, companyTransactionIdParam)
                return statement.executeUpdate()
            }
        }
    }

    private fun mapCompanyTransaction(rs: java.sql.ResultSet): CompanyTransaction {
        return CompanyTransaction(
            id = rs.getInt("id"),
            companyId = rs.getInt("company_id"),
            transactionCode = rs.getString("transaction_code"),
            transactionName = rs.getString("transaction_name"),
            transactionSubtitle = rs.getString("transaction_subtitle"),
            requiresCrewValidation = rs.getBoolean("requires_crew_validation"),
            inputMode = rs.getString("input_mode"),
            sortOrder = rs.getInt("sort_order"),
            status = rs.getString("status"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }
}
