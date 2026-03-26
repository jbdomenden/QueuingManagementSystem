package QueuingManagementSystem.controllers

import QueuingManagementSystem.models.DepartmentSummaryModel
import QueuingManagementSystem.models.HandlerPerformanceModel
import QueuingManagementSystem.models.QueueVolumeModel
import QueuingManagementSystem.queries.getDepartmentSummaryReportQuery
import QueuingManagementSystem.queries.getHandlerPerformanceReportQuery
import QueuingManagementSystem.queries.getQueueVolumeReportQuery
import marlow.systems.queuingsystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.*
import marlow.systems.queuingsystem.queries.*

class ReportController {
    fun getDepartmentSummary(departmentId: Int): DepartmentSummaryModel {
        ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(getDepartmentSummaryReportQuery).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> if (rs.next()) return DepartmentSummaryModel(
            rs.getInt("department_id"),
            rs.getInt("waiting_count"),
            rs.getInt("serving_count"),
            rs.getInt("completed_count")
        )
        } } } }
        return DepartmentSummaryModel(departmentId, 0, 0, 0)
    }
    fun getHandlerPerformance(departmentId: Int): MutableList<HandlerPerformanceModel> { val list = mutableListOf<HandlerPerformanceModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getHandlerPerformanceReportQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        HandlerPerformanceModel(rs.getInt("handler_id"), rs.getInt("handled_count"), rs.getInt("avg_service_seconds"))
    ) } } }; return list }
    fun getQueueVolume(departmentId: Int): MutableList<QueueVolumeModel> { val list = mutableListOf<QueueVolumeModel>(); ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        getQueueVolumeReportQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        QueueVolumeModel(
            rs.getInt(
                "queue_type_id"
            ), rs.getInt("issued_count")
        )
    ) } } }; return list }
}
