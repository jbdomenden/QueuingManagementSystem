package QueuingManagementSystem.models

import kotlinx.serialization.Serializable

@Serializable
data class WorkflowTemplateRequest(
    val id: Int? = null,
    val code: String,
    val name: String,
    val description: String? = null,
    val transaction_family: String? = null,
    val config_json: String? = null,
    val is_active: Boolean = true,
    val steps: List<WorkflowStepRequest> = emptyList(),
    val status_rules: List<WorkflowStatusRuleRequest> = emptyList()
)

@Serializable
data class WorkflowStepRequest(
    val step_code: String,
    val step_name: String,
    val step_order: Int,
    val status_on_enter: String? = null,
    val status_on_exit: String? = null,
    val is_required: Boolean = true,
    val is_active: Boolean = true,
    val config_json: String? = null
)

@Serializable
data class WorkflowStatusRuleRequest(
    val from_status: String,
    val to_status: String,
    val is_allowed: Boolean = true,
    val reason_required: Boolean = false,
    val is_active: Boolean = true
)

@Serializable
data class WorkflowAssignmentRequest(
    val template_id: Int,
    val department_id: Int? = null,
    val company_id: Int? = null,
    val queue_type_id: Int? = null,
    val company_transaction_id: Int? = null,
    val transaction_family: String? = null,
    val priority: Int = 0,
    val is_active: Boolean = true
)

@Serializable
data class WorkflowTemplateToggleRequest(
    val template_id: Int,
    val enabled: Boolean
)

@Serializable
data class WorkflowActiveLookupRequest(
    val department_id: Int,
    val queue_type_id: Int? = null,
    val company_id: Int? = null,
    val company_transaction_id: Int? = null,
    val transaction_family: String? = null
)

@Serializable
data class WorkflowTemplateModel(
    val id: Int,
    val code: String,
    val name: String,
    val description: String? = null,
    val transaction_family: String? = null,
    val config_json: String? = null,
    val is_active: Boolean,
    val created_at: String,
    val updated_at: String,
    val steps: List<WorkflowStepModel> = emptyList(),
    val status_rules: List<WorkflowStatusRuleModel> = emptyList()
)

@Serializable
data class WorkflowStepModel(
    val id: Int,
    val template_id: Int,
    val step_code: String,
    val step_name: String,
    val step_order: Int,
    val status_on_enter: String? = null,
    val status_on_exit: String? = null,
    val is_required: Boolean,
    val is_active: Boolean,
    val config_json: String? = null
)

@Serializable
data class WorkflowStatusRuleModel(
    val id: Int,
    val template_id: Int,
    val from_status: String,
    val to_status: String,
    val is_allowed: Boolean,
    val reason_required: Boolean,
    val is_active: Boolean
)

@Serializable
data class WorkflowAssignmentModel(
    val id: Int,
    val template_id: Int,
    val department_id: Int?,
    val company_id: Int?,
    val queue_type_id: Int?,
    val company_transaction_id: Int?,
    val transaction_family: String?,
    val priority: Int,
    val is_active: Boolean,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class WorkflowTemplateResponse(
    val template: WorkflowTemplateModel?,
    val result: GlobalCredentialResponse
)

fun WorkflowTemplateRequest.validate(): List<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (code.isBlank()) errors.add(GlobalCredentialResponse(400, false, "code is required"))
    if (name.isBlank()) errors.add(GlobalCredentialResponse(400, false, "name is required"))
    if (steps.isNotEmpty() && steps.any { it.step_code.isBlank() || it.step_name.isBlank() || it.step_order < 0 }) {
        errors.add(GlobalCredentialResponse(400, false, "all steps must include step_code, step_name, and non-negative step_order"))
    }
    if (status_rules.any { it.from_status.isBlank() || it.to_status.isBlank() }) {
        errors.add(GlobalCredentialResponse(400, false, "all status rules must include from_status and to_status"))
    }
    return errors
}

fun WorkflowAssignmentRequest.validate(): List<GlobalCredentialResponse> {
    val errors = mutableListOf<GlobalCredentialResponse>()
    if (template_id <= 0) errors.add(GlobalCredentialResponse(400, false, "template_id is required"))
    if (listOf(department_id, company_id, queue_type_id, company_transaction_id).all { it == null } && transaction_family.isNullOrBlank()) {
        errors.add(GlobalCredentialResponse(400, false, "at least one assignment target is required"))
    }
    return errors
}
