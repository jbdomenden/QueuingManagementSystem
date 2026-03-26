package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class DisplayBoardRequest(
    val id: Int? = null,
    val department_id: Int,
    val area_id: Int?,
    val code: String,
    val name: String,
    val is_active: Boolean = true
)

@Serializable
data class DisplayBoardWindowAssignmentRequest(
    val display_board_id: Int,
    val window_ids: List<Int>
)

@Serializable
data class DisplayBoardModel(
    val id: Int,
    val department_id: Int,
    val area_id: Int?,
    val code: String,
    val name: String,
    val is_active: Boolean
)

@Serializable
data class DisplayTicketSnapshot(
    val ticket_id: Int,
    val ticket_number: String,
    val queue_type_id: Int,
    val queue_type_name: String,
    val assigned_window_id: Int?,
    val assigned_window_name: String?,
    val status: String,
    val created_at: String
)

@Serializable
data class DisplaySnapshotResponse(
    val queued: List<QueuingManagementSystem.models.DisplayTicketSnapshot>,
    val now_serving: List<QueuingManagementSystem.models.DisplayTicketSnapshot>,
    val skipped: List<QueuingManagementSystem.models.DisplayTicketSnapshot>,
    val result: QueuingManagementSystem.models.GlobalCredentialResponse
)

fun QueuingManagementSystem.models.DisplayBoardRequest.validateDisplayBoardRequest(): MutableList<QueuingManagementSystem.models.GlobalCredentialResponse> {
    val errors = mutableListOf<QueuingManagementSystem.models.GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "department_id is required"
        )
    )
    if (code.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "code is required"
        )
    )
    if (name.isBlank()) errors.add(
        QueuingManagementSystem.models.GlobalCredentialResponse(
            400,
            false,
            "name is required"
        )
    )
    return errors
}
