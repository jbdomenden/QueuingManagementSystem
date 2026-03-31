package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.Company
import QueuingManagementSystem.models.CompanyCodeExistsResponse
import QueuingManagementSystem.models.CompanyKioskItem
import QueuingManagementSystem.models.CompanyRequest
import QueuingManagementSystem.queries.*

class CompanyController {
    fun getActiveCompaniesForKiosk(): List<CompanyKioskItem> {
        val list = mutableListOf<CompanyKioskItem>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveCompaniesForKioskQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            CompanyKioskItem(
                                id = rs.getInt("id"),
                                companyCode = rs.getString("company_code"),
                                companyName = rs.getString("company_short_name"),
                                companyDescription = rs.getString("company_full_name"),
                                cardSizeType = rs.getString("display_size"),
                                displayOrder = rs.getInt("sort_order"),
                                isActive = rs.getString("status") == "ACTIVE"
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun getCompanies(): List<Company> {
        val list = mutableListOf<Company>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompaniesQuery).use { statement ->
                statement.executeQuery().use { rs ->
                    while (rs.next()) list.add(mapCompany(rs))
                }
            }
        }
        return list
    }

    fun getCompanyById(companyIdParam: Int): Company {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompanyByIdQuery).use { statement ->
                statement.setInt(1, companyIdParam)
                statement.executeQuery().use { rs -> if (rs.next()) return mapCompany(rs) }
            }
        }
        return Company(0, "", "", "", "SMALL", 0, false, "", "")
    }

    fun getCompanyByCode(companyCode: String): CompanyCodeExistsResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompanyByCodeQuery).use { statement ->
                statement.setString(1, companyCode)
                statement.executeQuery().use { rs -> if (rs.next()) return CompanyCodeExistsResponse(true, rs.getInt("id")) }
            }
        }
        return CompanyCodeExistsResponse(false, 0)
    }

    fun getCompanyByCodeExceptId(companyCode: String, companyId: Int): CompanyCodeExistsResponse {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getCompanyByCodeExceptIdQuery).use { statement ->
                statement.setString(1, companyCode)
                statement.setInt(2, companyId)
                statement.executeQuery().use { rs -> if (rs.next()) return CompanyCodeExistsResponse(true, rs.getInt("id")) }
            }
        }
        return CompanyCodeExistsResponse(false, 0)
    }

    fun postCompany(request: CompanyRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postCompanyQuery).use { statement ->
                statement.setString(1, request.companyCode)
                statement.setString(2, request.companyName)
                statement.setString(3, request.companyDescription)
                statement.setString(4, request.cardSizeType)
                statement.setInt(5, request.displayOrder)
                statement.setString(6, if (request.isActive) "ACTIVE" else "INACTIVE")
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateCompany(companyIdParam: Int, request: CompanyRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateCompanyQuery).use { statement ->
                statement.setString(1, request.companyCode)
                statement.setString(2, request.companyName)
                statement.setString(3, request.companyDescription)
                statement.setString(4, request.cardSizeType)
                statement.setInt(5, request.displayOrder)
                statement.setString(6, if (request.isActive) "ACTIVE" else "INACTIVE")
                statement.setInt(7, companyIdParam)
                return statement.executeUpdate()
            }
        }
    }

    fun deactivateCompany(companyIdParam: Int): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(deactivateCompanyQuery).use { statement ->
                statement.setInt(1, companyIdParam)
                return statement.executeUpdate()
            }
        }
    }

    fun deleteCompany(companyIdParam: Int): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(deleteCompanyQuery).use { statement ->
                statement.setInt(1, companyIdParam)
                return statement.executeUpdate()
            }
        }
    }

    private fun mapCompany(rs: java.sql.ResultSet): Company {
        return Company(
            id = rs.getInt("id"),
            companyCode = rs.getString("company_code"),
            companyName = rs.getString("company_short_name"),
            companyDescription = rs.getString("company_full_name"),
            cardSizeType = rs.getString("display_size"),
            displayOrder = rs.getInt("sort_order"),
            isActive = rs.getString("status") == "ACTIVE",
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }
}
