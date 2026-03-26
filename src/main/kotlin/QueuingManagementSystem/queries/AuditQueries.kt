package QueuingManagementSystem.queries

const val postAuditLogQuery = "INSERT INTO audit_logs(actor_user_id, department_id, action, entity_name, entity_id, payload_json, created_at) VALUES(?, ?, ?, ?, ?, ?, NOW())"
const val getAuditLogsQuery = "SELECT id, actor_user_id, department_id, action, entity_name, entity_id, payload_json, created_at::text FROM audit_logs ORDER BY id DESC LIMIT 500"
const val getAuditLogsByDepartmentQuery = "SELECT id, actor_user_id, department_id, action, entity_name, entity_id, payload_json, created_at::text FROM audit_logs WHERE department_id = ? ORDER BY id DESC LIMIT 500"
