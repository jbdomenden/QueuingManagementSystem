package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: Int,
    val companyCode: String,
    val companyShortName: String,
    val companyFullName: String,
    val displaySize: String,
    val sortOrder: Int,
    val status: String,
    val createdAt: String,
    val updatedAt: String
)

@Serializable
data class CompanyRequest(
    val companyCode: String,
    val companyShortName: String,
    val companyFullName: String,
    val displaySize: String,
    val sortOrder: Int,
    val status: String = "ACTIVE"
)

@Serializable
data class CompanyResponse(
    val data: Company,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyListResponse(
    val data: List<Company>,
    val result: GlobalCredentialResponse
)

@Serializable
data class CompanyKioskItem(
    val id: Int,
    val companyCode: String,
    val companyShortName: String,
    val companyFullName: String,
    val displaySize: String,
    val sortOrder: Int
)

@Serializable
data class CompanyKioskListResponse(
    val data: List<CompanyKioskItem>,
    val result: GlobalCredentialResponse
)

fun CompanyRequest.validateCompanyRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()

    if (companyCode.isBlank()) errors.add(GlobalCredentialResponse(400, false, "companyCode is required"))
    if (companyShortName.isBlank()) errors.add(GlobalCredentialResponse(400, false, "companyShortName is required"))
    if (companyFullName.isBlank()) errors.add(GlobalCredentialResponse(400, false, "companyFullName is required"))
    if (displaySize !in listOf("BIG", "SMALL")) errors.add(GlobalCredentialResponse(400, false, "displaySize must be BIG or SMALL"))
    if (sortOrder < 0) errors.add(GlobalCredentialResponse(400, false, "sortOrder must be greater than or equal to 0"))
    if (status !in listOf("ACTIVE", "INACTIVE")) errors.add(GlobalCredentialResponse(400, false, "status must be ACTIVE or INACTIVE"))

    return errors
}
