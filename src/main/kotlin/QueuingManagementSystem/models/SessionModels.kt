package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class ActiveSessionModel(
    val session_id: String,
    val user_id: Int,
    val username: String,
    val full_name: String,
    val role: String,
    val department_id: Int?,
    val token_ref_hash: String,
    val login_at: String,
    val last_seen_at: String?,
    val logout_at: String?,
    val ip_address: String?,
    val user_agent: String?,
    val client_identifier: String?,
    val status: String,
    val revoked_reason: String?
)

@Serializable
data class SessionRevokeRequest(
    val session_ids: List<String>
)

fun SessionRevokeRequest.validate(): List<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (session_ids.isEmpty()) errors.add(GlobalCredentialResponse(400, false, "session_ids is required"))
    if (session_ids.any { it.isBlank() }) errors.add(GlobalCredentialResponse(400, false, "session_ids cannot contain blanks"))
    return errors
}
