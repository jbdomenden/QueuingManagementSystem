package QueuingManagementSystem.queries

const val postHandlerQuery = "INSERT INTO handlers(user_id, department_id, is_active) VALUES(?, ?, ?) RETURNING id"
const val updateHandlerQuery = "UPDATE handlers SET is_active = ? WHERE id = ?"
const val getHandlersByDepartmentQuery = "SELECT id, user_id, department_id, is_active FROM handlers WHERE department_id = ? ORDER BY id"

const val endActiveHandlerSessionQuery = "UPDATE handler_sessions SET logout_time = NOW(), last_seen_at = NOW(), status = 'OFFLINE', is_active = false WHERE handler_id = ? AND is_active = true"
const val postHandlerSessionQuery = "INSERT INTO handler_sessions(handler_id, user_id, window_id, login_time, last_seen_at, status, is_active) VALUES(?, ?, ?, NOW(), NOW(), 'ONLINE', true) RETURNING id"
const val getActiveHandlerSessionByHandlerIdQuery = "SELECT id, handler_id, user_id, window_id, login_time::text, last_seen_at::text, status FROM handler_sessions WHERE handler_id = ? AND is_active = true ORDER BY id DESC LIMIT 1"
const val getActiveHandlerSessionsByWindowQuery = "SELECT id, handler_id, user_id, window_id, login_time::text, last_seen_at::text, status FROM handler_sessions WHERE window_id = ? AND is_active = true ORDER BY id DESC"

const val validateHandlerWindowDepartmentQuery = """
SELECT h.id
FROM handlers h
JOIN windows w ON w.id = ?
WHERE h.id = ?
  AND h.is_active = true
  AND h.department_id = w.department_id
"""

const val getActiveHandlerWindowQuery = "SELECT window_id FROM handler_sessions WHERE handler_id = ? AND is_active = true ORDER BY id DESC LIMIT 1"

const val getActiveHandlersForQueueTypeQuery = """
SELECT hs.handler_id
FROM handler_sessions hs
JOIN window_queue_types wqt ON wqt.window_id = hs.window_id
WHERE hs.is_active = true
  AND hs.status = 'ONLINE'
  AND wqt.queue_type_id = ?
"""

const val getHandlerByUserIdQuery = "SELECT id, user_id, department_id, is_active FROM handlers WHERE user_id = ? AND is_active = true LIMIT 1"
