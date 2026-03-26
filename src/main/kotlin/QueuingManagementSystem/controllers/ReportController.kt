package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import marlow.systems.queuingsystem.models.*
import marlow.systems.queuingsystem.queries.*

class ReportController {
    fun getDepartmentSummary(departmentId: Int): QueuingManagementSystem.models.DepartmentSummaryModel {
        _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
            _root_ide_package_.QueuingManagementSystem.queries.getDepartmentSummaryReportQuery
        ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> if (rs.next()) return _root_ide_package_.QueuingManagementSystem.models.DepartmentSummaryModel(
            rs.getInt("department_id"),
            rs.getInt("waiting_count"),
            rs.getInt("serving_count"),
            rs.getInt("completed_count")
        )
        } } } }
        return DepartmentSummaryModel(departmentId, 0, 0, 0)
    }
    fun getHandlerPerformance(departmentId: Int): MutableList<QueuingManagementSystem.models.HandlerPerformanceModel> { val list = mutableListOf<QueuingManagementSystem.models.HandlerPerformanceModel>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getHandlerPerformanceReportQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        _root_ide_package_.QueuingManagementSystem.models.HandlerPerformanceModel(
            rs.getInt("handler_id"),
            rs.getInt("handled_count"),
            rs.getInt("avg_service_seconds")
        )
    ) } } }; return list }
    fun getQueueVolume(departmentId: Int): MutableList<QueuingManagementSystem.models.QueueVolumeModel> { val list = mutableListOf<QueuingManagementSystem.models.QueueVolumeModel>(); _root_ide_package_.QueuingManagementSystem.config.ConnectionPoolManager.getConnection().use { c -> c.prepareStatement(
        _root_ide_package_.QueuingManagementSystem.queries.getQueueVolumeReportQuery
    ).use { s -> s.setInt(1, departmentId); s.executeQuery().use { rs -> while (rs.next()) list.add(
        _root_ide_package_.QueuingManagementSystem.models.QueueVolumeModel(
            rs.getInt("queue_type_id"),
            rs.getInt("issued_count")
        )
    ) } } }; return list }
}
