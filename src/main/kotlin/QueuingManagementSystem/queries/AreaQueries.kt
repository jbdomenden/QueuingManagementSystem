package QueuingManagementSystem.queries

const val postAreaQuery = "INSERT INTO areas(department_id, name, is_active) VALUES(?, ?, ?) RETURNING id"
const val updateAreaQuery = "UPDATE areas SET name = ?, is_active = ? WHERE id = ?"
const val getAreasByDepartmentQuery = "SELECT id, department_id, name, is_active FROM areas WHERE department_id = ? ORDER BY id"
