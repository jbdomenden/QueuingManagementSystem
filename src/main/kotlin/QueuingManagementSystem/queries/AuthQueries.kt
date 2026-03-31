package QueuingManagementSystem.queries

const val getUserByUsernameForLoginQuery = """
SELECT id, username, password_hash, full_name, department_id, is_active
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

const val deactivateUserSessionsQuery = "UPDATE user_sessions SET token_status = 'REVOKED', logout_at = NOW() WHERE user_id = ? AND token_status = 'ACTIVE'"

const val postUserSessionQuery = """
INSERT INTO user_sessions(user_id, jwt_token, token_status, login_at, ip_address, user_agent)
VALUES (?, ?, 'ACTIVE', NOW(), ?, ?) RETURNING id
"""

const val getUserSessionByTokenQuery = """
SELECT id, user_id, token_status
FROM user_sessions
WHERE jwt_token = ?
LIMIT 1
"""

const val updateUserSessionLogoutByTokenQuery = "UPDATE user_sessions SET token_status = 'LOGGED_OUT', logout_at = NOW() WHERE jwt_token = ? AND token_status = 'ACTIVE'"

const val postLoginAuditQuery = """
INSERT INTO login_audit(user_id, username, success, event_at, ip_address, user_agent, reason)
VALUES (?, ?, ?, NOW(), ?, ?, ?)
"""

const val postFailedLoginAuditQuery = """
INSERT INTO failed_login_audit(username, attempted_at, ip_address, user_agent, reason)
VALUES (?, NOW(), ?, ?, ?)
"""

const val updateUserPasswordHashQuery = "UPDATE users SET password_hash = ? WHERE id = ?"

const val getActiveSessionByTokenWithUserQuery = """
SELECT s.user_id, s.token_status, u.department_id, u.username, u.full_name
FROM user_sessions s
JOIN users u ON u.id = s.user_id
WHERE s.jwt_token = ?
  AND s.token_status = 'ACTIVE'
  AND u.is_active = true
LIMIT 1
"""
