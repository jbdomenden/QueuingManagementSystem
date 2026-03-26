package QueuingManagementSystem.queries

const val getUserByUsernameAndPasswordQuery = """
SELECT id, full_name, role, department_id, auth_token
FROM users
WHERE username = ? AND password_hash = crypt(?, password_hash) AND is_active = true
"""

const val getUserByTokenQuery = """
SELECT id, department_id, role, auth_token
FROM users
WHERE auth_token = ? AND is_active = true
"""
