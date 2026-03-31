package QueuingManagementSystem.queries

const val getActiveSessionsByUserQuery = """
SELECT s.session_id, s.user_id, u.username, u.full_name, u.role, u.department_id,
       s.token_ref_hash, s.login_at, s.last_seen_at, s.logout_at,
       s.ip_address, s.user_agent, s.client_identifier, s.status, s.revoked_reason
FROM user_sessions s
JOIN users u ON u.id = s.user_id
WHERE s.user_id = ?
ORDER BY s.login_at DESC
"""

const val getActiveSessionsByDepartmentQuery = """
SELECT s.session_id, s.user_id, u.username, u.full_name, u.role, u.department_id,
       s.token_ref_hash, s.login_at, s.last_seen_at, s.logout_at,
       s.ip_address, s.user_agent, s.client_identifier, s.status, s.revoked_reason
FROM user_sessions s
JOIN users u ON u.id = s.user_id
WHERE (? IS NULL OR s.user_id = ?)
  AND (? IS NULL OR u.department_id = ?)
ORDER BY s.login_at DESC
LIMIT ? OFFSET ?
"""

const val revokeSessionByIdForAdminQuery = """
UPDATE user_sessions
SET status = 'REVOKED',
    logout_at = NOW(),
    revoked_reason = ?,
    updated_at = NOW()
WHERE session_id = ?
  AND status = 'ACTIVE'
"""

const val revokeUserOtherActiveSessionsQuery = """
UPDATE user_sessions
SET status = 'REVOKED',
    logout_at = NOW(),
    revoked_reason = 'USER_REVOKED_OTHER_SESSION',
    updated_at = NOW()
WHERE user_id = ?
  AND session_id <> ?
  AND status = 'ACTIVE'
"""

const val getSessionOwnerQuery = """
SELECT s.user_id, u.department_id
FROM user_sessions s
JOIN users u ON u.id = s.user_id
WHERE s.session_id = ?
LIMIT 1
"""
