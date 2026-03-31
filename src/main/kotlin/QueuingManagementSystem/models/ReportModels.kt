package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class DepartmentSummaryModel(
    val department_id: Int,
    val waiting_count: Int,
    val serving_count: Int,
    val completed_count: Int
)

@Serializable
data class HandlerPerformanceModel(
    val handler_id: Int,
    val handled_count: Int,
    val avg_service_seconds: Int
)

@Serializable
data class QueueVolumeModel(
    val queue_type_id: Int,
    val issued_count: Int
)

@Serializable
data class ArchivedQueueReportModel(
    val department_id: Int,
    val queue_type_id: Int,
    val status: String,
    val ticket_count: Int,
    val avg_waiting_seconds: Long,
    val avg_served_seconds: Long
)

@Serializable
data class DailyArchiveMetricsModel(
    val archive_date: String,
    val department_id: Int,
    val queue_type_id: Int,
    val company_id: Int?,
    val waiting_count: Int,
    val called_count: Int,
    val in_service_count: Int,
    val hold_count: Int,
    val no_show_count: Int,
    val completed_count: Int,
    val cancelled_count: Int,
    val transferred_count: Int,
    val override_count: Int,
    val avg_waiting_seconds: Long,
    val avg_serving_seconds: Long,
    val total_tickets: Int
)
