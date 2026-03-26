package QueuingManagementSystem.queries

const val getDepartmentSummaryReportQuery = """
SELECT department_id,
       COUNT(*) FILTER (WHERE status = 'WAITING') AS waiting_count,
       COUNT(*) FILTER (WHERE status IN ('CALLED', 'IN_SERVICE')) AS serving_count,
       COUNT(*) FILTER (WHERE status = 'COMPLETED') AS completed_count
FROM tickets
WHERE department_id = ?
GROUP BY department_id
"""

const val getHandlerPerformanceReportQuery = """
SELECT assigned_handler_id AS handler_id,
       COUNT(*) AS handled_count,
       COALESCE(AVG(EXTRACT(EPOCH FROM (completed_at - called_at)))::int, 0) AS avg_service_seconds
FROM tickets
WHERE department_id = ?
  AND status = 'COMPLETED'
  AND assigned_handler_id IS NOT NULL
GROUP BY assigned_handler_id
"""

const val getQueueVolumeReportQuery = """
SELECT queue_type_id, COUNT(*) AS issued_count
FROM tickets
WHERE department_id = ?
GROUP BY queue_type_id
"""
