package QueuingManagementSystem.auth.models

import java.time.Instant

data class QueueUser(
    val id: Int,
    val email: String,
    val passwordHash: String,
    val role: String,
    val fullName: String,
    val companyId: Int?,
    val departmentId: Int?,
    val isActive: Boolean,
    val forcePasswordChange: Boolean,
    val lastLoginAt: Instant?,
    val createdAt: Instant,
    val updatedAt: Instant
)

data class CreateQueueUserRequest(
    val email: String,
    val passwordHash: String,
    val role: String,
    val fullName: String,
    val companyId: Int?,
    val departmentId: Int?,
    val isActive: Boolean = true,
    val forcePasswordChange: Boolean = true
)
