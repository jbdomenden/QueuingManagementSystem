package QueuingManagementSystem.queries

const val postUserQuery = "INSERT INTO users(username, password_hash, full_name, role, department_id, is_active, auth_token) VALUES(?, crypt(?, gen_salt('bf')), ?, ?, ?, ?, ?) RETURNING id"
const val updateUserQuery = "UPDATE users SET full_name = ?, role = ?, department_id = ?, is_active = ? WHERE id = ?"
const val getUsersQuery = "SELECT id, username, full_name, role, department_id, is_active FROM users ORDER BY id"
const val getUsersByDepartmentQuery = "SELECT id, username, full_name, role, department_id, is_active FROM users WHERE department_id = ? ORDER BY id"

const val getUserByIdQuery = "SELECT id, username, full_name, role, department_id, is_active FROM users WHERE id = ?"
const val deleteUserDepartmentScopesQuery = "DELETE FROM user_department_scopes WHERE user_id = ?"
const val postUserDepartmentScopeQuery = "INSERT INTO user_department_scopes(user_id, department_id) VALUES(?, ?) ON CONFLICT (user_id, department_id) DO NOTHING"
