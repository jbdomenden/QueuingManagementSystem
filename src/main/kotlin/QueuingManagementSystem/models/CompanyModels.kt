package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class Company(
    val id: Int,
    val companyCode: String,
    val companyName: String,
    val companyDescription: String,
    val cardSizeType: String,
    val displayOrder: Int,
    val isActive: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val companyShortName: String = companyName,
    val companyFullName: String = companyDescription,
    val displaySize: String = cardSizeType,
    val sortOrder: Int = displayOrder,
    val status: String = if (isActive) "ACTIVE" else "INACTIVE"
)

@Serializable
data class CompanyRequest(
    val companyCode: String,
    val companyName: String,
    val companyDescription: String,
    val cardSizeType: String,
    val displayOrder: Int,
    val isActive: Boolean = true
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
    val companyName: String,
    val companyDescription: String,
    val cardSizeType: String,
    val displayOrder: Int,
    val isActive: Boolean,
    val companyShortName: String = companyName,
    val companyFullName: String = companyDescription,
    val displaySize: String = cardSizeType,
    val sortOrder: Int = displayOrder
)

@Serializable
data class CompanyKioskBoard(
    val title: String,
    val companies: List<CompanyKioskItem>
)

@Serializable
data class CompanyCodeExistsResponse(
    val exists: Boolean,
    val companyId: Int
)

fun CompanyRequest.validateCompanyRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()

    if (companyCode.isBlank()) errors.add(GlobalCredentialResponse(400, false, "companyCode is required"))
    if (companyName.isBlank()) errors.add(GlobalCredentialResponse(400, false, "companyName is required"))
    if (cardSizeType !in listOf("BIG", "SMALL")) errors.add(GlobalCredentialResponse(400, false, "cardSizeType must be BIG or SMALL"))
    if (displayOrder < 0) errors.add(GlobalCredentialResponse(400, false, "displayOrder must be greater than or equal to 0"))

    return errors
}
