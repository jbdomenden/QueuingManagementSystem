package QueuingManagementSystem.queries

const val postWorkflowTemplateQuery = """
INSERT INTO workflow_templates(code, name, description, transaction_family, config_json, is_active, created_by, updated_by, created_at, updated_at)
VALUES (?, ?, ?, ?, ?::jsonb, ?, ?, ?, NOW(), NOW())
RETURNING id, code, name, description, transaction_family, config_json::text, is_active, created_at::text, updated_at::text
"""

const val putWorkflowTemplateQuery = """
UPDATE workflow_templates
SET code = ?,
    name = ?,
    description = ?,
    transaction_family = ?,
    config_json = ?::jsonb,
    is_active = ?,
    updated_by = ?,
    updated_at = NOW()
WHERE id = ?
RETURNING id, code, name, description, transaction_family, config_json::text, is_active, created_at::text, updated_at::text
"""

const val getWorkflowTemplateByIdQuery = """
SELECT id, code, name, description, transaction_family, config_json::text, is_active, created_at::text, updated_at::text
FROM workflow_templates
WHERE id = ?
"""

const val getWorkflowTemplateByCodeQuery = """
SELECT id, code, name, description, transaction_family, config_json::text, is_active, created_at::text, updated_at::text
FROM workflow_templates
WHERE code = ?
"""

const val getWorkflowTemplatesQuery = """
SELECT id, code, name, description, transaction_family, config_json::text, is_active, created_at::text, updated_at::text
FROM workflow_templates
WHERE (? = true OR is_active = true)
ORDER BY id DESC
"""

const val putWorkflowTemplateEnabledQuery = """
UPDATE workflow_templates
SET is_active = ?, updated_by = ?, updated_at = NOW()
WHERE id = ?
"""

const val deleteWorkflowStepsByTemplateQuery = "DELETE FROM workflow_steps WHERE template_id = ?"

const val postWorkflowStepQuery = """
INSERT INTO workflow_steps(template_id, step_code, step_name, step_order, status_on_enter, status_on_exit, is_required, is_active, config_json, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, NOW(), NOW())
"""

const val getWorkflowStepsByTemplateQuery = """
SELECT id, template_id, step_code, step_name, step_order, status_on_enter, status_on_exit, is_required, is_active, config_json::text
FROM workflow_steps
WHERE template_id = ?
ORDER BY step_order ASC, id ASC
"""

const val deleteWorkflowStatusRulesByTemplateQuery = "DELETE FROM workflow_status_rules WHERE template_id = ?"

const val postWorkflowStatusRuleQuery = """
INSERT INTO workflow_status_rules(template_id, from_status, to_status, is_allowed, reason_required, is_active, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())
"""

const val getWorkflowStatusRulesByTemplateQuery = """
SELECT id, template_id, from_status, to_status, is_allowed, reason_required, is_active
FROM workflow_status_rules
WHERE template_id = ?
ORDER BY id ASC
"""

const val postWorkflowAssignmentQuery = """
INSERT INTO workflow_transaction_bindings(template_id, department_id, company_id, queue_type_id, company_transaction_id, transaction_family, priority, is_active, created_by, updated_by, created_at, updated_at)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())
RETURNING id, template_id, department_id, company_id, queue_type_id, company_transaction_id, transaction_family, priority, is_active, created_at::text, updated_at::text
"""

const val getWorkflowAssignmentsByTemplateQuery = """
SELECT id, template_id, department_id, company_id, queue_type_id, company_transaction_id, transaction_family, priority, is_active, created_at::text, updated_at::text
FROM workflow_transaction_bindings
WHERE template_id = ?
ORDER BY priority DESC, id DESC
"""

const val getActiveWorkflowTemplateByBindingQuery = """
SELECT wt.id, wt.code, wt.name, wt.description, wt.transaction_family, wt.config_json::text, wt.is_active, wt.created_at::text, wt.updated_at::text
FROM workflow_transaction_bindings wtb
JOIN workflow_templates wt ON wt.id = wtb.template_id
WHERE wtb.is_active = true
  AND wt.is_active = true
  AND (wtb.department_id IS NULL OR wtb.department_id = ?)
  AND (wtb.queue_type_id IS NULL OR wtb.queue_type_id = ?)
  AND (wtb.company_id IS NULL OR wtb.company_id = ?)
  AND (wtb.company_transaction_id IS NULL OR wtb.company_transaction_id = ?)
  AND (wtb.transaction_family IS NULL OR wtb.transaction_family = ?)
ORDER BY
  (CASE WHEN wtb.department_id IS NULL THEN 0 ELSE 1 END +
   CASE WHEN wtb.queue_type_id IS NULL THEN 0 ELSE 1 END +
   CASE WHEN wtb.company_id IS NULL THEN 0 ELSE 1 END +
   CASE WHEN wtb.company_transaction_id IS NULL THEN 0 ELSE 1 END +
   CASE WHEN wtb.transaction_family IS NULL THEN 0 ELSE 1 END) DESC,
  wtb.priority DESC,
  wtb.id DESC
LIMIT 1
"""

const val postWorkflowTemplateAuditQuery = """
INSERT INTO workflow_template_audit_logs(template_id, actor_user_id, action, payload_json, created_at)
VALUES (?, ?, ?, ?, NOW())
"""
