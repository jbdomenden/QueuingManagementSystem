package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class AuditLogModel(
    val id: Int,
    val actor_user_id: Int?,
    val department_id: Int?,
    val action: String,
    val entity_name: String,
    val entity_id: String,
    val payload_json: String?,
    val created_at: String
)
