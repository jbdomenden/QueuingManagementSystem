package marlow.systems.queuingsystem.queries

const val postQueueTypeQuery = "INSERT INTO queue_types(department_id, name, code, prefix, is_active) VALUES(?, ?, ?, ?, ?) RETURNING id"
const val updateQueueTypeQuery = "UPDATE queue_types SET name = ?, code = ?, prefix = ?, is_active = ? WHERE id = ?"
const val getQueueTypesByDepartmentQuery = "SELECT id, department_id, name, code, prefix, is_active FROM queue_types WHERE department_id = ? ORDER BY id"
