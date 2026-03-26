package marlow.systems.queuingsystem.models

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
