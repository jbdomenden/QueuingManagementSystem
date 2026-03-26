package marlow.systems.queuingsystem.models

import kotlinx.serialization.Serializable

@Serializable
data class GlobalCredentialResponse(
    val Code: Int,
    val Access: Boolean,
    val Status: String
)

@Serializable
data class IdResponse(
    val id: Int,
    val result: GlobalCredentialResponse
)

@Serializable
data class ListResponse<T>(
    val data: List<T>,
    val result: GlobalCredentialResponse
)

@Serializable
data class UserSessionModel(
    val user_id: Int = 0,
    val department_id: Int? = null,
    val role: String = "",
    val token: String = ""
)
