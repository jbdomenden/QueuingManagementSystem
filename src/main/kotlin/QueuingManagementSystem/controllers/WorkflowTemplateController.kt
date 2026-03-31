package QueuingManagementSystem.controllers

import QueuingManagementSystem.config.ConnectionPoolManager
import QueuingManagementSystem.models.*
import QueuingManagementSystem.queries.*

class WorkflowTemplateController {
    fun createTemplate(request: WorkflowTemplateRequest, actorUserId: Int): WorkflowTemplateModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var created: WorkflowTemplateModel? = null
                connection.prepareStatement(postWorkflowTemplateQuery).use { statement ->
                    statement.setString(1, request.code)
                    statement.setString(2, request.name)
                    statement.setString(3, request.description)
                    statement.setString(4, request.transaction_family)
                    statement.setString(5, request.config_json ?: "{}")
                    statement.setBoolean(6, request.is_active)
                    statement.setInt(7, actorUserId)
                    statement.setInt(8, actorUserId)
                    statement.executeQuery().use { rs -> if (rs.next()) created = mapTemplate(rs) }
                }
                val templateId = created?.id ?: throw IllegalStateException("template create failed")

                replaceTemplateSteps(connection, templateId, request.steps)
                replaceTemplateStatusRules(connection, templateId, request.status_rules)
                auditTemplateChange(connection, templateId, actorUserId, "WORKFLOW_TEMPLATE_CREATED", "{\"code\":\"${request.code}\"}")

                connection.commit()
                return getTemplateById(templateId)
            } catch (e: Exception) {
                connection.rollback()
                return null
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun updateTemplate(request: WorkflowTemplateRequest, actorUserId: Int): WorkflowTemplateModel? {
        val id = request.id ?: return null
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var updated: WorkflowTemplateModel? = null
                connection.prepareStatement(putWorkflowTemplateQuery).use { statement ->
                    statement.setString(1, request.code)
                    statement.setString(2, request.name)
                    statement.setString(3, request.description)
                    statement.setString(4, request.transaction_family)
                    statement.setString(5, request.config_json ?: "{}")
                    statement.setBoolean(6, request.is_active)
                    statement.setInt(7, actorUserId)
                    statement.setInt(8, id)
                    statement.executeQuery().use { rs -> if (rs.next()) updated = mapTemplate(rs) }
                }
                val templateId = updated?.id ?: throw IllegalStateException("template update failed")

                replaceTemplateSteps(connection, templateId, request.steps)
                replaceTemplateStatusRules(connection, templateId, request.status_rules)
                auditTemplateChange(connection, templateId, actorUserId, "WORKFLOW_TEMPLATE_UPDATED", "{\"id\":$templateId}")

                connection.commit()
                return getTemplateById(templateId)
            } catch (e: Exception) {
                connection.rollback()
                return null
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun setTemplateEnabled(templateId: Int, enabled: Boolean, actorUserId: Int): Boolean {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                val updated = connection.prepareStatement(putWorkflowTemplateEnabledQuery).use { statement ->
                    statement.setBoolean(1, enabled)
                    statement.setInt(2, actorUserId)
                    statement.setInt(3, templateId)
                    statement.executeUpdate() > 0
                }
                if (!updated) throw IllegalStateException("template not found")
                auditTemplateChange(connection, templateId, actorUserId, "WORKFLOW_TEMPLATE_TOGGLED", "{\"enabled\":$enabled}")
                connection.commit()
                return true
            } catch (e: Exception) {
                connection.rollback()
                return false
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun assignTemplate(request: WorkflowAssignmentRequest, actorUserId: Int): WorkflowAssignmentModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.autoCommit = false
            try {
                var assignment: WorkflowAssignmentModel? = null
                connection.prepareStatement(postWorkflowAssignmentQuery).use { statement ->
                    statement.setInt(1, request.template_id)
                    if (request.department_id == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, request.department_id)
                    if (request.company_id == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, request.company_id)
                    if (request.queue_type_id == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, request.queue_type_id)
                    if (request.company_transaction_id == null) statement.setNull(5, java.sql.Types.INTEGER) else statement.setInt(5, request.company_transaction_id)
                    statement.setString(6, request.transaction_family)
                    statement.setInt(7, request.priority)
                    statement.setBoolean(8, request.is_active)
                    statement.setInt(9, actorUserId)
                    statement.setInt(10, actorUserId)
                    statement.executeQuery().use { rs -> if (rs.next()) assignment = mapAssignment(rs) }
                }
                val mapped = assignment ?: throw IllegalStateException("assignment failed")
                auditTemplateChange(connection, request.template_id, actorUserId, "WORKFLOW_TEMPLATE_ASSIGNED", "{\"assignment_id\":${mapped.id}}")
                connection.commit()
                return mapped
            } catch (e: Exception) {
                connection.rollback()
                return null
            } finally {
                connection.autoCommit = true
            }
        }
    }

    fun listTemplates(includeInactive: Boolean): MutableList<WorkflowTemplateModel> {
        val list = mutableListOf<WorkflowTemplateModel>()
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getWorkflowTemplatesQuery).use { statement ->
                statement.setBoolean(1, includeInactive)
                statement.executeQuery().use { rs ->
                    while (rs.next()) {
                        val base = mapTemplate(rs)
                        list.add(base.copy(steps = getSteps(connection, base.id), status_rules = getStatusRules(connection, base.id)))
                    }
                }
            }
        }
        return list
    }

    fun getTemplateById(templateId: Int): WorkflowTemplateModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getWorkflowTemplateByIdQuery).use { statement ->
                statement.setInt(1, templateId)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        val base = mapTemplate(rs)
                        return base.copy(steps = getSteps(connection, base.id), status_rules = getStatusRules(connection, base.id))
                    }
                }
            }
        }
        return null
    }

    fun getActiveTemplate(request: WorkflowActiveLookupRequest): WorkflowTemplateModel? {
        ConnectionPoolManager.getConnection().use { connection ->
            connection.prepareStatement(getActiveWorkflowTemplateByBindingQuery).use { statement ->
                statement.setInt(1, request.department_id)
                if (request.queue_type_id == null) statement.setNull(2, java.sql.Types.INTEGER) else statement.setInt(2, request.queue_type_id)
                if (request.company_id == null) statement.setNull(3, java.sql.Types.INTEGER) else statement.setInt(3, request.company_id)
                if (request.company_transaction_id == null) statement.setNull(4, java.sql.Types.INTEGER) else statement.setInt(4, request.company_transaction_id)
                statement.setString(5, request.transaction_family)
                statement.executeQuery().use { rs ->
                    if (rs.next()) {
                        val base = mapTemplate(rs)
                        return base.copy(steps = getSteps(connection, base.id), status_rules = getStatusRules(connection, base.id))
                    }
                }
            }
        }
        return null
    }

    private fun replaceTemplateSteps(connection: java.sql.Connection, templateId: Int, steps: List<WorkflowStepRequest>) {
        connection.prepareStatement(deleteWorkflowStepsByTemplateQuery).use { statement ->
            statement.setInt(1, templateId)
            statement.executeUpdate()
        }
        if (steps.isEmpty()) return
        connection.prepareStatement(postWorkflowStepQuery).use { statement ->
            steps.sortedBy { it.step_order }.forEach { step ->
                statement.setInt(1, templateId)
                statement.setString(2, step.step_code)
                statement.setString(3, step.step_name)
                statement.setInt(4, step.step_order)
                statement.setString(5, step.status_on_enter)
                statement.setString(6, step.status_on_exit)
                statement.setBoolean(7, step.is_required)
                statement.setBoolean(8, step.is_active)
                statement.setString(9, step.config_json ?: "{}")
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun replaceTemplateStatusRules(connection: java.sql.Connection, templateId: Int, rules: List<WorkflowStatusRuleRequest>) {
        connection.prepareStatement(deleteWorkflowStatusRulesByTemplateQuery).use { statement ->
            statement.setInt(1, templateId)
            statement.executeUpdate()
        }
        if (rules.isEmpty()) return
        connection.prepareStatement(postWorkflowStatusRuleQuery).use { statement ->
            rules.forEach { rule ->
                statement.setInt(1, templateId)
                statement.setString(2, rule.from_status)
                statement.setString(3, rule.to_status)
                statement.setBoolean(4, rule.is_allowed)
                statement.setBoolean(5, rule.reason_required)
                statement.setBoolean(6, rule.is_active)
                statement.addBatch()
            }
            statement.executeBatch()
        }
    }

    private fun getSteps(connection: java.sql.Connection, templateId: Int): List<WorkflowStepModel> {
        val steps = mutableListOf<WorkflowStepModel>()
        connection.prepareStatement(getWorkflowStepsByTemplateQuery).use { statement ->
            statement.setInt(1, templateId)
            statement.executeQuery().use { rs -> while (rs.next()) steps.add(mapStep(rs)) }
        }
        return steps
    }

    private fun getStatusRules(connection: java.sql.Connection, templateId: Int): List<WorkflowStatusRuleModel> {
        val rules = mutableListOf<WorkflowStatusRuleModel>()
        connection.prepareStatement(getWorkflowStatusRulesByTemplateQuery).use { statement ->
            statement.setInt(1, templateId)
            statement.executeQuery().use { rs -> while (rs.next()) rules.add(mapStatusRule(rs)) }
        }
        return rules
    }

    private fun auditTemplateChange(connection: java.sql.Connection, templateId: Int, actorUserId: Int, action: String, payloadJson: String) {
        connection.prepareStatement(postWorkflowTemplateAuditQuery).use { statement ->
            statement.setInt(1, templateId)
            statement.setInt(2, actorUserId)
            statement.setString(3, action)
            statement.setString(4, payloadJson)
            statement.executeUpdate()
        }
        connection.prepareStatement(postAuditLogQuery).use { statement ->
            statement.setInt(1, actorUserId)
            statement.setNull(2, java.sql.Types.INTEGER)
            statement.setString(3, action)
            statement.setString(4, "workflow_templates")
            statement.setString(5, templateId.toString())
            statement.setString(6, payloadJson)
            statement.executeUpdate()
        }
    }

    private fun mapTemplate(rs: java.sql.ResultSet): WorkflowTemplateModel {
        return WorkflowTemplateModel(
            id = rs.getInt("id"),
            code = rs.getString("code"),
            name = rs.getString("name"),
            description = rs.getString("description"),
            transaction_family = rs.getString("transaction_family"),
            config_json = rs.getString("config_json"),
            is_active = rs.getBoolean("is_active"),
            created_at = rs.getString("created_at"),
            updated_at = rs.getString("updated_at")
        )
    }

    private fun mapStep(rs: java.sql.ResultSet): WorkflowStepModel {
        return WorkflowStepModel(
            id = rs.getInt("id"),
            template_id = rs.getInt("template_id"),
            step_code = rs.getString("step_code"),
            step_name = rs.getString("step_name"),
            step_order = rs.getInt("step_order"),
            status_on_enter = rs.getString("status_on_enter"),
            status_on_exit = rs.getString("status_on_exit"),
            is_required = rs.getBoolean("is_required"),
            is_active = rs.getBoolean("is_active"),
            config_json = rs.getString("config_json")
        )
    }

    private fun mapStatusRule(rs: java.sql.ResultSet): WorkflowStatusRuleModel {
        return WorkflowStatusRuleModel(
            id = rs.getInt("id"),
            template_id = rs.getInt("template_id"),
            from_status = rs.getString("from_status"),
            to_status = rs.getString("to_status"),
            is_allowed = rs.getBoolean("is_allowed"),
            reason_required = rs.getBoolean("reason_required"),
            is_active = rs.getBoolean("is_active")
        )
    }

    private fun mapAssignment(rs: java.sql.ResultSet): WorkflowAssignmentModel {
        return WorkflowAssignmentModel(
            id = rs.getInt("id"),
            template_id = rs.getInt("template_id"),
            department_id = rs.getInt("department_id").let { if (rs.wasNull()) null else it },
            company_id = rs.getInt("company_id").let { if (rs.wasNull()) null else it },
            queue_type_id = rs.getInt("queue_type_id").let { if (rs.wasNull()) null else it },
            company_transaction_id = rs.getInt("company_transaction_id").let { if (rs.wasNull()) null else it },
            transaction_family = rs.getString("transaction_family"),
            priority = rs.getInt("priority"),
            is_active = rs.getBoolean("is_active"),
            created_at = rs.getString("created_at"),
            updated_at = rs.getString("updated_at")
        )
    }
}
