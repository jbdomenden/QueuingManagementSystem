package QueuingManagementSystem.queries

const val postDisplayBoardQuery = "INSERT INTO display_boards(department_id, area_id, code, name, is_active) VALUES(?, ?, ?, ?, ?) RETURNING id"
const val updateDisplayBoardQuery = "UPDATE display_boards SET area_id = ?, code = ?, name = ?, is_active = ? WHERE id = ?"
const val getDisplayBoardsQuery = "SELECT id, department_id, area_id, code, name, is_active FROM display_boards ORDER BY id"
const val getDisplayBoardByIdQuery = "SELECT id, department_id, area_id, code, name, is_active FROM display_boards WHERE id = ?"
const val deleteDisplayBoardWindowsQuery = "DELETE FROM display_board_windows WHERE display_board_id = ?"
const val postDisplayBoardWindowQuery = "INSERT INTO display_board_windows(display_board_id, window_id) VALUES(?, ?)"
const val getDisplayBoardWindowsQuery = """
SELECT w.id, w.department_id, w.area_id, w.code, w.name, w.is_active
FROM display_board_windows dbw
JOIN windows w ON w.id = dbw.window_id
WHERE dbw.display_board_id = ?
ORDER BY w.id
"""

const val getQueuedTicketsForDisplayQuery = """
SELECT t.id, t.ticket_number, t.queue_type_id, qt.name AS queue_type_name, t.assigned_window_id,
       w.name AS assigned_window_name, t.status, t.created_at::text,
       t.created_at::text AS queued_at,
       EXTRACT(EPOCH FROM (NOW() - t.created_at))::bigint AS waiting_seconds,
       NULL::bigint AS served_seconds
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status = 'WAITING'
  AND t.archived = false
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
       w.name AS assigned_window_name, t.status, t.created_at::text,
       t.created_at::text AS queued_at,
       CASE WHEN t.called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.called_at - t.created_at))::bigint
            ELSE EXTRACT(EPOCH FROM (NOW() - t.created_at))::bigint END AS waiting_seconds,
       CASE
         WHEN t.completed_at IS NOT NULL AND t.service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.completed_at - t.service_started_at))::bigint
         WHEN t.completed_at IS NOT NULL AND t.called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.completed_at - t.called_at))::bigint
         ELSE NULL
       END AS served_seconds
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status IN ('CALLED', 'IN_SERVICE')
  AND t.archived = false
  AND t.assigned_window_id IN (
      SELECT dbw.window_id
      FROM display_board_windows dbw
      WHERE dbw.display_board_id = ?
  )
ORDER BY t.called_at DESC NULLS LAST
"""

const val getSkippedTicketsForDisplayQuery = """
SELECT t.id, t.ticket_number, t.queue_type_id, qt.name AS queue_type_name, t.assigned_window_id,
       w.name AS assigned_window_name, t.status, t.created_at::text,
       t.created_at::text AS queued_at,
       CASE WHEN t.called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.called_at - t.created_at))::bigint
            ELSE EXTRACT(EPOCH FROM (NOW() - t.created_at))::bigint END AS waiting_seconds,
       CASE
         WHEN t.completed_at IS NOT NULL AND t.service_started_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.completed_at - t.service_started_at))::bigint
         WHEN t.completed_at IS NOT NULL AND t.called_at IS NOT NULL THEN EXTRACT(EPOCH FROM (t.completed_at - t.called_at))::bigint
         ELSE NULL
       END AS served_seconds
FROM tickets t
JOIN queue_types qt ON qt.id = t.queue_type_id
LEFT JOIN windows w ON w.id = t.assigned_window_id
WHERE t.status = 'SKIPPED'
  AND t.archived = false
  AND t.assigned_window_id IN (
      SELECT dbw.window_id
      FROM display_board_windows dbw
      WHERE dbw.display_board_id = ?
  )
ORDER BY t.updated_at DESC
LIMIT 20
"""
