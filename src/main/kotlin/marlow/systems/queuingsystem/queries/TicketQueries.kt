package marlow.systems.queuingsystem.queries

const val getQueueTypeWithDepartmentByIdQuery = "SELECT id, department_id, prefix FROM queue_types WHERE id = ?"
const val upsertQueueDailySequenceQuery = """
INSERT INTO queue_daily_sequences(queue_type_id, sequence_date, current_value)
VALUES(?, CURRENT_DATE, 1)
ON CONFLICT (queue_type_id, sequence_date)
DO UPDATE SET current_value = queue_daily_sequences.current_value + 1
RETURNING current_value
"""

const val postTicketQuery = """
INSERT INTO tickets(ticket_number, department_id, queue_type_id, kiosk_id, status, created_at, updated_at)
VALUES(?, ?, ?, ?, 'WAITING', NOW(), NOW())
RETURNING id
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
    updated_at = NOW()
FROM candidate
WHERE t.id = candidate.id
RETURNING t.id, t.ticket_number, t.department_id, t.queue_type_id, t.kiosk_id, t.assigned_window_id, t.assigned_handler_id,
          t.status, t.created_at::text, t.called_at::text, t.completed_at::text
"""

const val updateTicketToInServiceQuery = "UPDATE tickets SET status = 'IN_SERVICE', updated_at = NOW() WHERE id = ? AND assigned_handler_id = ?"
const val updateTicketToSkippedQuery = "UPDATE tickets SET status = 'SKIPPED', updated_at = NOW() WHERE id = ? AND assigned_handler_id = ?"
const val updateTicketToCalledRecallQuery = "UPDATE tickets SET status = 'CALLED', updated_at = NOW() WHERE id = ? AND assigned_handler_id = ?"
const val updateTicketToCompletedQuery = "UPDATE tickets SET status = 'COMPLETED', completed_at = NOW(), updated_at = NOW() WHERE id = ? AND assigned_handler_id = ?"
const val postTicketLogQuery = "INSERT INTO ticket_logs(ticket_id, action, actor_handler_id, created_at, payload_json) VALUES(?, ?, ?, NOW(), ?)"
