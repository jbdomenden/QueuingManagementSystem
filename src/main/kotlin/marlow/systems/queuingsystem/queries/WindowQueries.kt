package marlow.systems.queuingsystem.queries

const val postWindowQuery = "INSERT INTO windows(department_id, area_id, code, name, is_active) VALUES(?, ?, ?, ?, ?) RETURNING id"
const val updateWindowQuery = "UPDATE windows SET area_id = ?, code = ?, name = ?, is_active = ? WHERE id = ?"
const val getWindowsByDepartmentQuery = "SELECT id, department_id, area_id, code, name, is_active FROM windows WHERE department_id = ? ORDER BY id"
const val deleteWindowQueueTypesByWindowQuery = "DELETE FROM window_queue_types WHERE window_id = ?"
const val postWindowQueueTypeQuery = "INSERT INTO window_queue_types(window_id, queue_type_id) VALUES(?, ?)"
