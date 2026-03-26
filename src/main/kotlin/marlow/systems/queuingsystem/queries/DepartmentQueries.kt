package marlow.systems.queuingsystem.queries

const val postDepartmentQuery = "INSERT INTO departments(code, name, is_active) VALUES(?, ?, ?) RETURNING id"
const val updateDepartmentQuery = "UPDATE departments SET code = ?, name = ?, is_active = ? WHERE id = ?"
const val getDepartmentsQuery = "SELECT id, code, name, is_active FROM departments ORDER BY id"
