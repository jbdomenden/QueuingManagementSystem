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
data class DisplayWindowModel(
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
    val created_at: String,
    val queuedAt: String? = null,
    val waitingSeconds: Long? = null,
    val waitingDisplay: String? = null,
    val servedSeconds: Long? = null,
    val servedDisplay: String? = null
)

@Serializable
data class DisplaySnapshotResponse(
    val display: DisplayBoardModel?,
    val windows: List<DisplayWindowModel>,
    val queued: List<DisplayTicketSnapshot>,
    val now_serving: List<DisplayTicketSnapshot>,
    val skipped: List<DisplayTicketSnapshot>,
    val result: GlobalCredentialResponse
)

@Serializable
data class DisplayFilterParams(
    val department_id: Int? = null,
    val area_id: Int? = null,
    val floor_id: Int? = null,
    val company_id: Int? = null
)

@Serializable
data class DisplayWindowTicketState(
    val window_id: Int,
    val window_name: String,
    val ticket_id: Int,
    val ticket_number: String,
    val queue_type_id: Int,
    val queue_type_name: String,
    val status: String
)

@Serializable
data class QueueTypeWaitingCount(
    val queue_type_id: Int,
    val queue_type_name: String,
    val waiting_count: Int
)

@Serializable
data class StatusCount(
    val status: String,
    val count: Int
)

@Serializable
data class DisplayAggregateSnapshotResponse(
    val display: DisplayBoardModel?,
    val filters: DisplayFilterParams,
    val floor_filter_supported: Boolean,
    val current_called_tickets: List<DisplayWindowTicketState>,
    val current_serving_tickets: List<DisplayWindowTicketState>,
    val waiting_counts_by_queue_type: List<QueueTypeWaitingCount>,
    val counts_by_status: List<StatusCount>,
    val hold_count: Int,
    val no_show_count: Int,
    val visitor_count: Int?,
    val special_count: Int?,
    val result: GlobalCredentialResponse
)

fun DisplayBoardRequest.validateDisplayBoardRequest(): MutableList<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (department_id <= 0) errors.add(GlobalCredentialResponse(400, false, "department_id is required"))
    if (code.isBlank()) errors.add(GlobalCredentialResponse(400, false, "code is required"))
    if (name.isBlank()) errors.add(GlobalCredentialResponse(400, false, "name is required"))
    return errors
}


@Serializable
data class WallboardFilterOption(
    val id: String,
    val label: String
)

@Serializable
data class WallboardQueueRow(
    val ticketNumber: String,
    val terminalNumber: String? = null,
    val transactionName: String
)

@Serializable
data class WallboardCounts(
    val onQueue: Int,
    val noShow: Int,
    val onHold: Int,
    val visitorSupplier: Int
)

@Serializable
data class DisplayWallboardResponse(
    val filterOptions: List<WallboardFilterOption>,
    val selectedFilter: String,
    val counts: WallboardCounts,
    val called: List<WallboardQueueRow>,
    val onQueue: List<WallboardQueueRow>,
    val noShow: List<WallboardQueueRow>,
    val onHold: List<WallboardQueueRow>,
    val visitorSupplier: List<WallboardQueueRow>,
    val result: GlobalCredentialResponse
)
