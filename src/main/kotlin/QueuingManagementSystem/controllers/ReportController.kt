package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*

class ReportController {
    fun getDepartmentSummary(departmentId: Int): DepartmentSummaryModel {
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getDepartmentSummaryReportQuery).use { s ->
                s.setInt(1, departmentId)
                s.executeQuery().use { rs ->
                    if (rs.next()) {
                        return DepartmentSummaryModel(
                            rs.getInt("department_id"),
                            rs.getInt("waiting_count"),
                            rs.getInt("serving_count"),
                            rs.getInt("completed_count")
                        )
                    }
                }
            }
        }
        return DepartmentSummaryModel(departmentId, 0, 0, 0)
    }

    fun getHandlerPerformance(departmentId: Int): MutableList<HandlerPerformanceModel> {
        val list = mutableListOf<HandlerPerformanceModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getHandlerPerformanceReportQuery).use { s ->
                s.setInt(1, departmentId)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(HandlerPerformanceModel(rs.getInt("handler_id"), rs.getInt("handled_count"), rs.getInt("avg_service_seconds")))
                    }
                }
            }
        }
        return list
    }

    fun getQueueVolume(departmentId: Int): MutableList<QueueVolumeModel> {
        val list = mutableListOf<QueueVolumeModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getQueueVolumeReportQuery).use { s ->
                s.setInt(1, departmentId)
                s.executeQuery().use { rs -> while (rs.next()) list.add(QueueVolumeModel(rs.getInt("queue_type_id"), rs.getInt("issued_count"))) }
            }
        }
        return list
    }

    fun getArchivedQueueReportByDepartment(departmentId: Int, dateFrom: String, dateTo: String): MutableList<ArchivedQueueReportModel> {
        val list = mutableListOf<ArchivedQueueReportModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getArchivedQueueReportByDepartmentQuery).use { s ->
                s.setInt(1, departmentId)
                s.setString(2, dateFrom)
                s.setString(3, dateTo)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            ArchivedQueueReportModel(
                                rs.getInt("department_id"),
                                rs.getInt("queue_type_id"),
                                rs.getString("status"),
                                rs.getInt("ticket_count"),
                                rs.getLong("avg_waiting_seconds"),
                                rs.getLong("avg_served_seconds")
                            )
                        )
                    }
                }
            }
        }
        return list
    }

    fun getArchivedQueueReport(dateFrom: String, dateTo: String): MutableList<ArchivedQueueReportModel> {
        val list = mutableListOf<ArchivedQueueReportModel>()
        ConnectionPoolManager.getConnection().use { c ->
            c.prepareStatement(getArchivedQueueReportAllDepartmentsQuery).use { s ->
                s.setString(1, dateFrom)
                s.setString(2, dateTo)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            ArchivedQueueReportModel(
                                rs.getInt("department_id"),
                                rs.getInt("queue_type_id"),
                                rs.getString("status"),
                                rs.getInt("ticket_count"),
                                rs.getLong("avg_waiting_seconds"),
                                rs.getLong("avg_served_seconds")
                            )
                        )
                    }
                }
            }
        }
        return list
    }
}
