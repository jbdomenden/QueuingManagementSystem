package QueuingManagementSystem.queries

const val getQueueTypeWithDepartmentByIdQuery = "SELECT id, department_id, prefix FROM queue_types WHERE id = ? AND is_active = true"
const val getKioskByIdAndDepartmentQueueTypeQuery = """
SELECT k.id
FROM kiosks k
JOIN kiosk_queue_types kqt ON kqt.kiosk_id = k.id
WHERE k.id = ?
  AND kqt.queue_type_id = ?
  AND k.is_active = true
"""

const val getQueueDailySequenceQuery = "SELECT current_value FROM queue_daily_sequences WHERE queue_type_id = ? AND sequence_date = CURRENT_DATE"
const val upsertQueueDailySequenceQuery = """
INSERT INTO queue_daily_sequences(queue_type_id, sequence_date, current_value)
VALUES(?, CURRENT_DATE, 1)
ON CONFLICT (queue_type_id, sequence_date)
DO UPDATE SET current_value = queue_daily_sequences.current_value + 1
RETURNING current_value
"""

const val postTicketQuery = """
INSERT INTO tickets(ticket_number, department_id, queue_type_id, kiosk_id, status, created_at, last_action_at, updated_at)
VALUES(?, ?, ?, ?, 'WAITING', NOW(), NOW(), NOW())
RETURNING id, ticket_number, department_id, queue_type_id, kiosk_id, assigned_window_id, assigned_handler_id,
          status, created_at::text, called_at::text, completed_at::text
"""

const val getLiveTicketsByDepartmentQuery = "SELECT id, ticket_number, department_id, queue_type_id, kiosk_id, assigned_window_id, assigned_handler_id, status, created_at::text, called_at::text, completed_at::text FROM tickets WHERE department_id = ? ORDER BY created_at DESC LIMIT 100"

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
RETURNING t.id, t.ticket_number, t.department_id, t.queue_type_id, t.kiosk_id, t.assigned_window_id, t.assigned_handler_id,
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
"""

const val updateTicketToSkippedQuery = """
UPDATE tickets
SET status = 'SKIPPED',
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
"""

const val updateTicketToCalledRecallQuery = """
UPDATE tickets
SET status = 'CALLED',
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
"""

const val updateTicketToCompletedQuery = """
UPDATE tickets
SET status = 'COMPLETED',
    completed_at = NOW(),
    last_action_at = NOW(),
    updated_at = NOW()
WHERE id = ?
  AND assigned_handler_id = ?
"""

const val postTicketLogQuery = "INSERT INTO ticket_logs(ticket_id, action, actor_handler_id, created_at, payload_json) VALUES(?, ?, ?, NOW(), ?)"

const val getQueuedTicketsForDisplayQuery = """
SELECT t.id, t.ticket_number, t.queue_type_id, qt.name AS queue_type_name, t.assigned_window_id,
       w.name AS assigned_window_name, t.status, t.created_at::text
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status = 'WAITING'
  AND EXISTS (
      SELECT 1
      FROM display_board_windows dbw
      JOIN window_queue_types wqt ON wqt.window_id = dbw.window_id
      WHERE dbw.display_board_id = ?
        AND wqt.queue_type_id = t.queue_type_id
  )
ORDER BY t.created_at ASC
"""

const val getNowServingTicketsForDisplayQuery = """
SELECT t.id, t.ticket_number, t.queue_type_id, qt.name AS queue_type_name, t.assigned_window_id,
       w.name AS assigned_window_name, t.status, t.created_at::text
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status IN ('CALLED', 'IN_SERVICE')
  AND t.assigned_window_id IN (
      SELECT dbw.window_id
      FROM display_board_windows dbw
      WHERE dbw.display_board_id = ?
  )
ORDER BY t.called_at DESC NULLS LAST
"""

const val getSkippedTicketsForDisplayQuery = """
SELECT t.id, t.ticket_number, t.queue_type_id, qt.name AS queue_type_name, t.assigned_window_id,
       w.name AS assigned_window_name, t.status, t.created_at::text
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status = 'SKIPPED'
  AND t.assigned_window_id IN (
      SELECT dbw.window_id
      FROM display_board_windows dbw
      WHERE dbw.display_board_id = ?
  )
ORDER BY t.updated_at DESC
LIMIT 20
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
