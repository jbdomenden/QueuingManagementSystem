package QueuingManagementSystem.queries

const val getQueueTypeWithDepartmentByIdQuery = "SELECT id, department_id, prefix, company_id FROM queue_types WHERE id = ? AND is_active = true"
const val getKioskByIdAndDepartmentQueueTypeQuery = """
SELECT k.id
FROM kiosks k
JOIN kiosk_queue_types kqt ON kqt.kiosk_id = k.id
WHERE k.id = ?
  AND kqt.queue_type_id = ?
  AND k.is_active = true
"""

const val getQueueDailySequenceQuery = "SELECT current_value FROM queue_daily_sequences WHERE queue_type_id = ? AND sequence_date = CURRENT_DATE"
const val getActiveCompanyByIdQuery = "SELECT id, status FROM companies WHERE id = ?"
const val upsertQueueDailySequenceQuery = """
INSERT INTO queue_daily_sequences(queue_type_id, sequence_date, current_value)
VALUES(?, CURRENT_DATE, 1)
ON CONFLICT (queue_type_id, sequence_date)
DO UPDATE SET current_value = queue_daily_sequences.current_value + 1
RETURNING current_value
"""

const val getCompanyTransactionDetailsByIdQuery = """
SELECT ct.id,
       ct.company_id,
       ct.transaction_name,
       ct.status,
       c.status AS company_status
FROM company_transactions ct
JOIN companies c ON c.id = ct.company_id
WHERE ct.id = ?
"""

const val postTicketQuery = """
INSERT INTO tickets(ticket_number, department_id, queue_type_id, company_transaction_id, kiosk_id, service_date, status, created_at, last_action_at, updated_at, archived)
VALUES(?, ?, ?, ?, ?, CURRENT_DATE, 'WAITING', NOW(), NOW(), NOW(), false)
RETURNING id, ticket_number, department_id, queue_type_id, company_transaction_id, kiosk_id, assigned_window_id, assigned_handler_id,
          status, created_at::text, called_at::text, completed_at::text
"""

const val getTicketPrintableDetailsByIdQuery = """
SELECT t.id AS ticket_id, t.ticket_number, t.department_id, d.name AS department_name,
       t.queue_type_id, qt.name AS queue_type_name, c.company_full_name AS company_name,
       ct.transaction_name AS company_transaction_name, t.status,
       TO_CHAR(t.created_at, 'YYYY-MM-DD') AS queue_date,
       TO_CHAR(t.created_at, 'HH24:MI:SS') AS queue_time,
       t.created_at::text AS queued_at
FROM tickets t
JOIN departments d ON d.id = t.department_id
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN companies c ON c.id = qt.company_id
LEFT JOIN company_transactions ct ON ct.id = t.company_transaction_id
WHERE t.id = ?
"""

const val getDepartmentAndQueueTypeByTicketIdQuery = """
SELECT t.department_id, t.queue_type_id
FROM tickets t
WHERE t.id = ?
"""

const val getLiveTicketsByDepartmentQuery = """
SELECT id, ticket_number, department_id, queue_type_id, company_transaction_id, kiosk_id, assigned_window_id, assigned_handler_id,
       status, created_at::text, called_at::text, completed_at::text
FROM tickets
WHERE department_id = ?
  AND archived = false
ORDER BY created_at DESC
LIMIT 100
"""

const val getWaitingTicketForHandlerCallNextWithLockingQuery = """
WITH current_window AS (
    SELECT hs.window_id
    FROM handler_sessions hs
    WHERE hs.handler_id = ?
      AND hs.is_active = true
    ORDER BY hs.id DESC
    LIMIT 1
), candidate AS (
    SELECT t.id
    FROM tickets t
    JOIN window_queue_types wqt ON wqt.queue_type_id = t.queue_type_id
    JOIN current_window cw ON cw.window_id = wqt.window_id
    WHERE t.status = 'WAITING'
      AND t.archived = false
    ORDER BY t.created_at ASC
    FOR UPDATE SKIP LOCKED
    LIMIT 1
)
UPDATE tickets t
SET status = 'CALLED',
    assigned_handler_id = ?,
    assigned_window_id = (SELECT window_id FROM current_window),
    called_at = NOW(),
    last_action_at = NOW(),
    updated_at = NOW()
FROM candidate
WHERE t.id = candidate.id
RETURNING t.id, t.ticket_number, t.department_id, t.queue_type_id, t.company_transaction_id, t.kiosk_id, t.assigned_window_id, t.assigned_handler_id,
          t.status, t.created_at::text, t.called_at::text, t.completed_at::text
"""

const val updateTicketToInServiceQuery = """
UPDATE tickets
SET status = 'IN_SERVICE',
    service_started_at = NOW(),
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
  AND archived = false
"""

const val updateTicketToSkippedQuery = """
UPDATE tickets
SET status = 'SKIPPED',
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
  AND archived = false
"""

const val updateTicketToCalledRecallQuery = """
UPDATE tickets
SET status = 'CALLED',
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
  AND archived = false
"""

const val updateTicketToCompletedQuery = """
UPDATE tickets
SET status = 'COMPLETED',
    completed_at = NOW(),
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
  AND archived = false
"""

const val postTicketLogQuery = "INSERT INTO ticket_logs(ticket_id, action, actor_handler_id, created_at, payload_json) VALUES(?, ?, ?, NOW(), ?)"

const val markTicketsArchivedByServiceDateQuery = """
UPDATE tickets
SET archived = true,
    archived_at = NOW(),
    updated_at = NOW()
WHERE service_date = ?::date
  AND archived = false
  AND (? IS NULL OR department_id = ?)
"""

const val getArchivedTicketsByDepartmentAndDateRangeQuery = """
SELECT id, ticket_number, department_id, queue_type_id, status, service_date::text, created_at::text,
       CASE WHEN called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (called_at - created_at))::bigint
            ELSE EXTRACT(EPOCH FROM (NOW() - created_at))::bigint END AS waiting_seconds,
       CASE
         WHEN completed_at IS NOT NULL AND service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - service_started_at))::bigint
         WHEN completed_at IS NOT NULL AND called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - called_at))::bigint
         ELSE NULL
       END AS served_seconds
FROM tickets
WHERE archived = true
  AND department_id = ?
  AND service_date BETWEEN ?::date AND ?::date
  AND (? IS NULL OR queue_type_id = ?)
  AND (? IS NULL OR status = ?)
ORDER BY created_at DESC
"""

const val getArchivedTicketsByDateRangeQuery = """
SELECT id, ticket_number, department_id, queue_type_id, status, service_date::text, created_at::text,
       CASE WHEN called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (called_at - created_at))::bigint
            ELSE EXTRACT(EPOCH FROM (NOW() - created_at))::bigint END AS waiting_seconds,
       CASE
         WHEN completed_at IS NOT NULL AND service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - service_started_at))::bigint
         WHEN completed_at IS NOT NULL AND called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - called_at))::bigint
         ELSE NULL
       END AS served_seconds
FROM tickets
WHERE archived = true
  AND service_date BETWEEN ?::date AND ?::date
  AND (? IS NULL OR queue_type_id = ?)
  AND (? IS NULL OR status = ?)
ORDER BY created_at DESC
"""

const val getArchivedTicketByIdQuery = """
SELECT id, ticket_number, department_id, queue_type_id, status, service_date::text, created_at::text,
       CASE WHEN called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (called_at - created_at))::bigint
            ELSE EXTRACT(EPOCH FROM (NOW() - created_at))::bigint END AS waiting_seconds,
       CASE
         WHEN completed_at IS NOT NULL AND service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - service_started_at))::bigint
         WHEN completed_at IS NOT NULL AND called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (completed_at - called_at))::bigint
         ELSE NULL
       END AS served_seconds
FROM tickets
WHERE id = ?
  AND archived = true
"""

const val getActiveHandlerSessionsByQueueTypeEligibilityQuery = """
SELECT hs.id, hs.handler_id, hs.user_id, hs.window_id
FROM handler_sessions hs
JOIN window_queue_types wqt ON wqt.window_id = hs.window_id
WHERE hs.is_active = true
  AND hs.status = 'ONLINE'
  AND wqt.queue_type_id = ?
"""

const val getDisplayIdsForQueueTypeQuery = """
SELECT DISTINCT dbw.display_board_id
FROM display_board_windows dbw
JOIN window_queue_types wqt ON wqt.window_id = dbw.window_id
WHERE wqt.queue_type_id = ?
"""

const val getDisplayIdsByHandlerQuery = """
SELECT DISTINCT dbw.display_board_id
FROM handler_sessions hs
JOIN display_board_windows dbw ON dbw.window_id = hs.window_id
WHERE hs.handler_id = ? AND hs.is_active = true
"""
