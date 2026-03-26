package marlow.systems.queuingsystem.queries

const val postDisplayBoardQuery = "INSERT INTO display_boards(department_id, area_id, code, name, is_active) VALUES(?, ?, ?, ?, ?) RETURNING id"
const val updateDisplayBoardQuery = "UPDATE display_boards SET area_id = ?, code = ?, name = ?, is_active = ? WHERE id = ?"
const val getDisplayBoardsQuery = "SELECT id, department_id, area_id, code, name, is_active FROM display_boards ORDER BY id"
const val deleteDisplayBoardWindowsQuery = "DELETE FROM display_board_windows WHERE display_board_id = ?"
const val postDisplayBoardWindowQuery = "INSERT INTO display_board_windows(display_board_id, window_id) VALUES(?, ?)"

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
