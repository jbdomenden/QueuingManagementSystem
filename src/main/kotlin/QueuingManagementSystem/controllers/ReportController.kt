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

    fun getDailyArchiveMetrics(dateFrom: String, dateTo: String, departmentId: Int?): MutableList<DailyArchiveMetricsModel> {
        val list = mutableListOf<DailyArchiveMetricsModel>()
        ConnectionPoolManager.getConnection().use { c ->
            val query = if (departmentId == null) getDailyArchiveMetricsAllDepartmentsQuery else getDailyArchiveMetricsByDepartmentQuery
            c.prepareStatement(query).use { s ->
                s.setString(1, dateFrom)
                s.setString(2, dateTo)
                if (departmentId != null) s.setInt(3, departmentId)
                s.executeQuery().use { rs ->
                    while (rs.next()) {
                        list.add(
                            DailyArchiveMetricsModel(
                                archive_date = rs.getString("archive_date"),
                                department_id = rs.getInt("department_id"),
                                queue_type_id = rs.getInt("queue_type_id"),
                                company_id = rs.getInt("company_id").let { if (it <= 0 || rs.wasNull()) null else it },
                                waiting_count = rs.getInt("waiting_count"),
                                called_count = rs.getInt("called_count"),
                                in_service_count = rs.getInt("in_service_count"),
                                hold_count = rs.getInt("hold_count"),
                                no_show_count = rs.getInt("no_show_count"),
                                completed_count = rs.getInt("completed_count"),
                                cancelled_count = rs.getInt("cancelled_count"),
                                transferred_count = rs.getInt("transferred_count"),
                                override_count = rs.getInt("override_count"),
                                avg_waiting_seconds = rs.getLong("avg_waiting_seconds"),
                                avg_serving_seconds = rs.getLong("avg_serving_seconds"),
                                total_tickets = rs.getInt("total_tickets")
                            )
                        )
                    }
                }
            }
        }
        return list
    }
}
