package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class ManagedUserModel(
    val id: Int,
    val email: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val forcePasswordChange: Boolean,
    val companyId: Int?,
    val departmentId: Int?,
    val permissions: List<String>
)

@Serializable
data class UserCreateRequest(
    val email: String,
    val password: String,
    val fullName: String,
    val role: String,
    val isActive: Boolean = true,
    val forcePasswordChange: Boolean = false,
    val companyId: Int? = null,
    val departmentId: Int? = null
)

@Serializable
data class UserUpdateRequest(
    val fullName: String,
    val role: String,
    val isActive: Boolean,
    val companyId: Int? = null,
    val departmentId: Int? = null,
    val forcePasswordChange: Boolean = false
)

@Serializable data class UserStatusPatchRequest(val isActive: Boolean)
@Serializable data class UserRolePatchRequest(val role: String)
@Serializable data class UserAccessAssignment(val accessKey: String, val isAllowed: Boolean)
@Serializable data class UserAccessPatchRequest(val access: List<UserAccessAssignment>)
@Serializable data class ResetPasswordRequest(val password: String? = null)
@Serializable data class ResetPasswordResponse(val temporaryPassword: String, val result: GlobalCredentialResponse)

@Serializable
data class AssetModel(
    val id: Int,
    val assetTag: String,
    val assetName: String,
    val assetType: String,
    val deviceKey: String?,
    val ipAddress: String?,
    val macAddress: String?,
    val status: String,
    val assignedDepartmentId: Int?,
    val assignedCompanyId: Int?,
    val location: String?,
    val notes: String?
)

@Serializable
data class AssetRequest(
    val assetTag: String,
    val assetName: String,
    val assetType: String,
    val deviceKey: String? = null,
    val ipAddress: String? = null,
    val macAddress: String? = null,
    val status: String,
    val assignedDepartmentId: Int? = null,
    val assignedCompanyId: Int? = null,
    val location: String? = null,
    val notes: String? = null
)

@Serializable data class AssetStatusPatchRequest(val status: String)
