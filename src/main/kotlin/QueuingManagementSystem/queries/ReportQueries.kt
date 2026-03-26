package QueuingManagementSystem.queries

const val getDepartmentSummaryReportQuery = """
SELECT department_id,
       COUNT(*) FILTER (WHERE status = 'WAITING' AND archived = false) AS waiting_count,
       COUNT(*) FILTER (WHERE status IN ('CALLED', 'IN_SERVICE') AND archived = false) AS serving_count,
       COUNT(*) FILTER (WHERE status = 'COMPLETED' AND archived = false) AS completed_count
FROM tickets
WHERE department_id = ?
GROUP BY department_id
"""

const val getHandlerPerformanceReportQuery = """
SELECT assigned_handler_id AS handler_id,
       COUNT(*) AS handled_count,
       COALESCE(AVG(EXTRACT(EPOCH FROM (
           completed_at - COALESCE(service_started_at, called_at)
       )))::int, 0) AS avg_service_seconds
FROM tickets
WHERE department_id = ?
  AND status = 'COMPLETED'
  AND assigned_handler_id IS NOT NULL
  AND archived = false
GROUP BY assigned_handler_id
"""

const val getQueueVolumeReportQuery = """
SELECT queue_type_id, COUNT(*) AS issued_count
FROM tickets
WHERE department_id = ?
  AND archived = false
GROUP BY queue_type_id
"""

const val getArchivedQueueReportByDepartmentQuery = """
SELECT department_id, queue_type_id, status,
       COUNT(*) AS ticket_count,
       COALESCE(AVG(
           CASE WHEN called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (called_at - created_at))::bigint ELSE 0 END
       )::bigint, 0) AS avg_waiting_seconds,
       COALESCE(AVG(
           CASE
             WHEN completed_at IS NOT NULL AND service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - service_started_at))::bigint
             WHEN completed_at IS NOT NULL AND called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - called_at))::bigint
             ELSE 0
           END
       )::bigint, 0) AS avg_served_seconds
FROM tickets
WHERE archived = true
  AND department_id = ?
  AND service_date BETWEEN ?::date AND ?::date
GROUP BY department_id, queue_type_id, status
ORDER BY queue_type_id, status
"""

const val getArchivedQueueReportAllDepartmentsQuery = """
SELECT department_id, queue_type_id, status,
       COUNT(*) AS ticket_count,
       COALESCE(AVG(
           CASE WHEN called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (called_at - created_at))::bigint ELSE 0 END
       )::bigint, 0) AS avg_waiting_seconds,
       COALESCE(AVG(
           CASE
             WHEN completed_at IS NOT NULL AND service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - service_started_at))::bigint
             WHEN completed_at IS NOT NULL AND called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - called_at))::bigint
             ELSE 0
           END
       )::bigint, 0) AS avg_served_seconds
FROM tickets
WHERE archived = true
  AND service_date BETWEEN ?::date AND ?::date
GROUP BY department_id, queue_type_id, status
ORDER BY department_id, queue_type_id, status
"""
