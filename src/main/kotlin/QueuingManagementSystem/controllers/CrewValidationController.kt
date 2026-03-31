package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.CrewValidationResponse
import QueuingManagementSystem.queries.getCompanyTransactionDetailsByIdQuery

class CrewValidationController {
    fun validateCrewIdentifier(companyIdParam: Int, companyTransactionIdParam: Int, identifierValueParam: String, identifierTypeParam: String): CrewValidationResponse {
        ConnectionPoolManager.getConnection().use { c ->
            var transactionCompanyId = 0
            var transactionStatus = ""
            var companyStatus = ""
            var requiresCrewValidation = false
            var inputMode = "NONE"
            c.prepareStatement(getCompanyTransactionDetailsByIdQuery).use { s ->
                s.setInt(1, companyTransactionIdParam)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        transactionCompanyId = rs.getInt("company_id")
                        transactionStatus = rs.getString("status")
                        companyStatus = rs.getString("company_status")
                        requiresCrewValidation = rs.getBoolean("requires_crew_validation")
                        inputMode = rs.getString("input_mode")
                    }
                }
            }

            if (transactionCompanyId != companyIdParam || transactionStatus != "ACTIVE" || companyStatus != "ACTIVE") {
                return CrewValidationResponse(false, null, null, null, "Invalid or inactive transaction/company")
            }
            if (!requiresCrewValidation) return CrewValidationResponse(false, null, null, null, "Crew validation not required for this transaction")
            if (inputMode == "KEYPAD" && identifierTypeParam != "KEYPAD") return CrewValidationResponse(false, null, null, null, "Transaction expects KEYPAD input")
            if (inputMode == "RFID" && identifierTypeParam != "RFID") return CrewValidationResponse(false, null, null, null, "Transaction expects RFID input")

            c.prepareStatement("SELECT id, full_name FROM users WHERE is_active = true AND (username = ? OR auth_token = ?) LIMIT 1").use { s ->
                s.setString(1, identifierValueParam)
                s.setString(2, identifierValueParam)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        return CrewValidationResponse(
                            success = true,
                            crewId = rs.getInt("id"),
                            crewDisplayName = rs.getString("full_name"),
                            crewCompany = null,
                            message = "Validation successful"
                        )
                    }
                }
            }
        }
        return CrewValidationResponse(false, null, null, null, "Crew not found")
    }
}
