package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class CompanyTransaction(
    val id: Int,
    val companyId: Int,
    val transactionCode: String,
    val transactionName: String,
    val transactionSubtitle: String? = null,
    val requiresCrewValidation: Boolean = false,
    val inputMode: String = "NONE",
    val sortOrder: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CompanyTransactionRequest(
    val companyId: Int,
    val transactionCode: String,
    val transactionName: String,
    val transactionSubtitle: String? = null,
    val requiresCrewValidation: Boolean = false,
    val inputMode: String = "NONE",
    val sortOrder: Int = 0,
    val status: String = "ACTIVE"
)

@Serializable
data class CompanyTransactionToggleRequest(
    val status: String
)

@Serializable
data class CompanyTransactionResponse(
    val data: CompanyTransaction,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyTransactionListResponse(
    val data: List<CompanyTransaction>,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyTransactionKioskItem(
    val id: Int,
    val companyId: Int,
    val transactionCode: String,
    val transactionName: String,
    val transactionSubtitle: String? = null,
    val requiresCrewValidation: Boolean = false,
    val inputMode: String = "NONE",
    val sortOrder: Int
)

@Serializable
data class CompanyTransactionKioskListResponse(
    val data: List<CompanyTransactionKioskItem>,
    val result: GlobalCredentialResponse
)

fun CompanyTransactionRequest.validateCompanyTransactionRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (companyId <= 0) errors.add(GlobalCredentialResponse(400, false, "companyId is required"))
    if (transactionCode.isBlank()) errors.add(GlobalCredentialResponse(400, false, "transactionCode is required"))
    if (transactionName.isBlank()) errors.add(GlobalCredentialResponse(400, false, "transactionName is required"))
    if (sortOrder < 0) errors.add(GlobalCredentialResponse(400, false, "sortOrder must be greater than or equal to 0"))
    if (status !in listOf("ACTIVE", "INACTIVE")) errors.add(GlobalCredentialResponse(400, false, "status must be ACTIVE or INACTIVE"))
    if (inputMode !in listOf("NONE", "KEYPAD", "RFID", "BOTH")) errors.add(GlobalCredentialResponse(400, false, "inputMode must be NONE, KEYPAD, RFID, or BOTH"))
    return errors
}

fun CompanyTransactionToggleRequest.validateCompanyTransactionToggleRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (status !in listOf("ACTIVE", "INACTIVE")) errors.add(GlobalCredentialResponse(400, false, "status must be ACTIVE or INACTIVE"))
    return errors
}
