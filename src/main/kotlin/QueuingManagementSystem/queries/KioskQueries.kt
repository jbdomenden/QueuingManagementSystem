package QueuingManagementSystem.queries

const val postKioskQuery = "INSERT INTO kiosks(department_id, name, is_active) VALUES(?, ?, ?) RETURNING id"
const val updateKioskQuery = "UPDATE kiosks SET name = ?, is_active = ? WHERE id = ?"
const val getKiosksQuery = "SELECT id, department_id, name, is_active FROM kiosks ORDER BY id"
const val getKioskDepartmentByIdQuery = "SELECT department_id FROM kiosks WHERE id = ?"
const val deleteKioskQueueTypesByKioskQuery = "DELETE FROM kiosk_queue_types WHERE kiosk_id = ?"
const val postKioskQueueTypeQuery = "INSERT INTO kiosk_queue_types(kiosk_id, queue_type_id) VALUES(?, ?)"
