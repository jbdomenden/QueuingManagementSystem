package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class CrewValidationRequest(
    val companyId: Int,
    val companyTransactionId: Int,
    val identifierValue: String,
    val identifierType: String
)

@Serializable
data class CrewValidationResponse(
    val success: Boolean,
    val crewId: Int? = null,
    val crewDisplayName: String? = null,
    val crewCompany: String? = null,
    val message: String
)

fun CrewValidationRequest.validateCrewValidationRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (companyId <= 0) errors.add(GlobalCredentialResponse(400, false, "companyId is required"))
    if (companyTransactionId <= 0) errors.add(GlobalCredentialResponse(400, false, "companyTransactionId is required"))
    if (identifierValue.isBlank()) errors.add(GlobalCredentialResponse(400, false, "identifierValue is required"))
    if (identifierType !in listOf("KEYPAD", "RFID")) errors.add(GlobalCredentialResponse(400, false, "identifierType must be KEYPAD or RFID"))
    return errors
}
