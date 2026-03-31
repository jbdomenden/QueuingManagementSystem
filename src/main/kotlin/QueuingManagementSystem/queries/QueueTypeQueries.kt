package QueuingManagementSystem.queries

const val postQueueTypeQuery = "INSERT INTO queue_types(department_id, company_id, name, code, prefix, is_active) VALUES(?, ?, ?, ?, ?, ?) RETURNING id"
const val updateQueueTypeQuery = "UPDATE queue_types SET department_id = ?, company_id = ?, name = ?, code = ?, prefix = ?, is_active = ? WHERE id = ?"
const val getQueueTypesByDepartmentQuery = "SELECT id, department_id, company_id, name, code, prefix, is_active, NULL::int AS kiosk_id FROM queue_types WHERE department_id = ? ORDER BY id"
const val getQueueTypesByCompanyIdQuery = """
SELECT qt.id,
       qt.department_id,
       qt.company_id,
       qt.name,
       qt.code,
       qt.prefix,
       qt.is_active,
       MIN(k.id) AS kiosk_id
FROM queue_types qt
JOIN kiosk_queue_types kqt ON kqt.queue_type_id = qt.id
JOIN kiosks k ON k.id = kqt.kiosk_id AND k.is_active = true
WHERE qt.company_id = ?
  AND qt.is_active = true
GROUP BY qt.id, qt.department_id, qt.company_id, qt.name, qt.code, qt.prefix, qt.is_active
ORDER BY qt.name ASC
"""
const val getQueueTypeByIdQuery = "SELECT id, department_id, company_id, name, code, prefix, is_active, NULL::int AS kiosk_id FROM queue_types WHERE id = ?"
