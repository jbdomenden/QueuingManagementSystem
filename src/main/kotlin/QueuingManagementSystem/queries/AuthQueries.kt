package QueuingManagementSystem.queries

const val getUserByUsernameForLoginQuery = """
SELECT id, username, password_hash, full_name, role, department_id, is_active
FROM users
WHERE username = ?
LIMIT 1
"""

const val getUserByIdForAuthQuery = """
SELECT id, username, full_name, department_id, is_active
FROM users
WHERE id = ?
LIMIT 1
"""

const val getPermissionsByUserIdQuery = """
SELECT DISTINCT p.code
FROM user_role_assignments ura
JOIN roles r ON r.id = ura.role_id
JOIN role_permissions rp ON rp.role_id = r.id
JOIN permissions p ON p.id = rp.permission_id
WHERE ura.user_id = ?
"""

const val revokeOtherActiveUserSessionsQuery = """
UPDATE user_sessions
SET status = 'REVOKED',
    logout_at = NOW(),
    revoked_reason = ?,
    updated_at = NOW()
WHERE user_id = ?
  AND status = 'ACTIVE'
"""

const val postUserSessionQuery = """
INSERT INTO user_sessions(session_id, user_id, token_ref_hash, status, login_at, last_seen_at, ip_address, user_agent, client_identifier)
VALUES (?, ?, ?, 'ACTIVE', NOW(), NOW(), ?, ?, ?) RETURNING session_id
"""

const val updateUserSessionLogoutBySessionIdQuery = """
UPDATE user_sessions
SET status = 'LOGGED_OUT',
    logout_at = NOW(),
    updated_at = NOW()
WHERE session_id = ?
  AND token_ref_hash = ?
  AND status = 'ACTIVE'
"""

const val postLoginAuditQuery = """
INSERT INTO login_audit(user_id, username, success, event_at, ip_address, user_agent, reason)
VALUES (?, ?, ?, NOW(), ?, ?, ?)
"""

const val postFailedLoginAuditQuery = """
INSERT INTO failed_login_audit(username, attempted_at, ip_address, user_agent, reason)
VALUES (?, NOW(), ?, ?, ?)
"""

const val postSessionLifecycleAuditQuery = """
INSERT INTO audit_logs(actor_user_id, department_id, action, entity_name, entity_id, payload_json, created_at)
VALUES (?, ?, ?, ?, ?, ?, NOW())
"""

const val updateUserPasswordHashQuery = "UPDATE users SET password_hash = ? WHERE id = ?"

const val getActiveSessionByRefWithUserQuery = """
SELECT s.session_id, s.user_id, s.status, u.department_id, u.username, u.full_name, u.role
FROM user_sessions s
JOIN users u ON u.id = s.user_id
WHERE s.session_id = ?
  AND s.token_ref_hash = ?
  AND s.status = 'ACTIVE'
  AND u.is_active = true
LIMIT 1
"""

const val touchSessionLastSeenByIdQuery = """
UPDATE user_sessions
SET last_seen_at = NOW(), updated_at = NOW()
WHERE session_id = ?
  AND status = 'ACTIVE'
  AND (last_seen_at IS NULL OR last_seen_at < NOW() - INTERVAL '30 seconds')
"""

const val markSessionExpiredByIdQuery = """
UPDATE user_sessions
SET status = 'EXPIRED',
    logout_at = NOW(),
    revoked_reason = 'TOKEN_EXPIRED',
    updated_at = NOW()
WHERE session_id = ?
  AND status = 'ACTIVE'
"""
