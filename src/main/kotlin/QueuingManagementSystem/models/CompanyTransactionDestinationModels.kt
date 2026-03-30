package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class CompanyTransactionDestination(
    val id: Int,
    val companyTransactionId: Int,
    val destinationCode: String,
    val destinationName: String,
    val destinationSubtitle: String? = null,
    val queueTypeId: Int? = null,
    val sortOrder: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CompanyTransactionDestinationRequest(
    val companyTransactionId: Int,
    val destinationCode: String,
    val destinationName: String,
    val destinationSubtitle: String? = null,
    val queueTypeId: Int? = null,
    val sortOrder: Int = 0,
    val status: String = "ACTIVE"
)

@Serializable
data class CompanyTransactionDestinationToggleRequest(
    val status: String
)

@Serializable
data class CompanyTransactionDestinationResponse(
    val data: CompanyTransactionDestination,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyTransactionDestinationListResponse(
    val data: List<CompanyTransactionDestination>,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyTransactionDestinationKioskItem(
    val id: Int,
    val companyTransactionId: Int,
    val destinationCode: String,
    val destinationName: String,
    val destinationSubtitle: String? = null,
    val queueTypeId: Int? = null,
    val sortOrder: Int
)

@Serializable
data class CompanyTransactionDestinationKioskListResponse(
    val data: List<CompanyTransactionDestinationKioskItem>,
    val result: GlobalCredentialResponse
)

fun CompanyTransactionDestinationRequest.validateCompanyTransactionDestinationRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (companyTransactionId <= 0) errors.add(GlobalCredentialResponse(400, false, "companyTransactionId is required"))
    if (destinationCode.isBlank()) errors.add(GlobalCredentialResponse(400, false, "destinationCode is required"))
    if (destinationName.isBlank()) errors.add(GlobalCredentialResponse(400, false, "destinationName is required"))
    if (sortOrder < 0) errors.add(GlobalCredentialResponse(400, false, "sortOrder must be greater than or equal to 0"))
    if (status !in listOf("ACTIVE", "INACTIVE")) errors.add(GlobalCredentialResponse(400, false, "status must be ACTIVE or INACTIVE"))
    if (queueTypeId != null && queueTypeId <= 0) errors.add(GlobalCredentialResponse(400, false, "queueTypeId must be greater than 0"))
    return errors
}

fun CompanyTransactionDestinationToggleRequest.validateCompanyTransactionDestinationToggleRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (status !in listOf("ACTIVE", "INACTIVE")) errors.add(GlobalCredentialResponse(400, false, "status must be ACTIVE or INACTIVE"))
    return errors
}
