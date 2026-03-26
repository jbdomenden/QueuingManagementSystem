package QueuingManagementSystem.queries

const val postUserQuery = "INSERT INTO users(username, password_hash, full_name, role, department_id, is_active, auth_token) VALUES(?, crypt(?, gen_salt('bf')), ?, ?, ?, ?, ?) RETURNING id"
const val updateUserQuery = "UPDATE users SET full_name = ?, role = ?, department_id = ?, is_active = ? WHERE id = ?"
const val getUsersQuery = "SELECT id, username, full_name, role, department_id, is_active FROM users ORDER BY id"
const val getUsersByDepartmentQuery = "SELECT id, username, full_name, role, department_id, is_active FROM users WHERE department_id = ? ORDER BY id"
