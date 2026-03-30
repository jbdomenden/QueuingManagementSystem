package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.Company
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
                                companyShortName = rs.getString("company_short_name"),
                                companyFullName = rs.getString("company_full_name"),
                                displaySize = rs.getString("display_size"),
                                sortOrder = rs.getInt("sort_order")
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
        return Company(0, "", "", "", "SMALL", 0, "INACTIVE", "", "")
    }

    fun postCompany(request: CompanyRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(postCompanyQuery).use { statement ->
                statement.setString(1, request.companyCode)
                statement.setString(2, request.companyShortName)
                statement.setString(3, request.companyFullName)
                statement.setString(4, request.displaySize)
                statement.setInt(5, request.sortOrder)
                statement.setString(6, request.status)
                statement.executeQuery().use { rs -> if (rs.next()) return rs.getInt("id") }
            }
        }
        return 0
    }

    fun updateCompany(companyIdParam: Int, request: CompanyRequest): Int {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(updateCompanyQuery).use { statement ->
                statement.setString(1, request.companyCode)
                statement.setString(2, request.companyShortName)
                statement.setString(3, request.companyFullName)
                statement.setString(4, request.displaySize)
                statement.setInt(5, request.sortOrder)
                statement.setString(6, request.status)
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

    private fun mapCompany(rs: java.sql.ResultSet): Company {
        return Company(
            id = rs.getInt("id"),
            companyCode = rs.getString("company_code"),
            companyShortName = rs.getString("company_short_name"),
            companyFullName = rs.getString("company_full_name"),
            displaySize = rs.getString("display_size"),
            sortOrder = rs.getInt("sort_order"),
            status = rs.getString("status"),
            createdAt = rs.getString("created_at"),
            updatedAt = rs.getString("updated_at")
        )
    }
}
