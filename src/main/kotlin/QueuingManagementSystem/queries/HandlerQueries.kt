package QueuingManagementSystem.queries

const val postHandlerQuery = "INSERT INTO handlers(user_id, department_id, is_active) VALUES(?, ?, ?) RETURNING id"
const val updateHandlerQuery = "UPDATE handlers SET is_active = ? WHERE id = ?"
const val getHandlersByDepartmentQuery = "SELECT id, user_id, department_id, is_active FROM handlers WHERE department_id = ? ORDER BY id"
const val endActiveHandlerSessionQuery = "UPDATE handler_sessions SET ended_at = NOW(), is_active = false WHERE handler_id = ? AND is_active = true"
const val postHandlerSessionQuery = "INSERT INTO handler_sessions(handler_id, window_id, started_at, is_active) VALUES(?, ?, NOW(), true) RETURNING id"
const val getActiveHandlerWindowQuery = "SELECT window_id FROM handler_sessions WHERE handler_id = ? AND is_active = true ORDER BY id DESC LIMIT 1"
const val getActiveHandlersForQueueTypeQuery = """
SELECT hs.handler_id
FROM handler_sessions hs
JOIN window_queue_types wqt ON wqt.window_id = hs.window_id
WHERE hs.is_active = true
  AND hs.ended_at IS NULL
  AND wqt.queue_type_id = ?
"""
