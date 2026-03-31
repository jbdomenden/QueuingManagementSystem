# Route Inventory

This inventory reflects the currently wired Ktor routes in `src/main/kotlin/QueuingManagementSystem/routes`.

## Auth
- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/validate`
- `POST /auth/change-password`
- `GET /auth/me`

## Sessions
- `GET /sessions/me` (permission: `session_view_self`)
- `GET /sessions/admin` (permission: `session_view_all`; scope constrained unless super admin)
- `POST /sessions/me/revoke-others` (permission: `session_revoke_self_other`)
- `POST /sessions/{sessionId}/revoke` (permission: `session_revoke_any`; scope constrained unless super admin)

## Core Admin Entities
- `POST /departments/create` (permission: `department_manage`)
- `PUT /departments/update` (permission: `department_manage`)
- `GET /departments/list` (permission: `department_view`)

- `POST /areas/create` (permission: `area_manage`; scoped)
- `PUT /areas/update` (permission: `area_manage`; scoped)
- `GET /areas/list/{departmentId}` (permission: `area_manage`; scoped)

- `POST /queue-types/create` (permission: `queue_type_manage`; scoped)
- `PUT /queue-types/update` (permission: `queue_type_manage`; scoped)
- `GET /queue-types/company/{companyId}` (permission: `queue_type_manage`)
- `GET /queue-types/list/{departmentId}` (permission: `queue_type_manage`; scoped)

- `POST /windows/create` (permission: `window_manage`; scoped)
- `PUT /windows/update` (permission: `window_manage`; scoped)
- `GET /windows/list/{departmentId}` (permission: `window_manage`; scoped)
- `POST /windows/assign-queue-types` (permission: `window_manage`; scoped + queue type department checks)

- `POST /kiosks/create` (role: `SUPERADMIN` or `DEPARTMENT_ADMIN`; scoped)
- `PUT /kiosks/update` (role: `SUPERADMIN` or `DEPARTMENT_ADMIN`; scoped)
- `POST /kiosks/assign-queue-types` (role: `SUPERADMIN` or `DEPARTMENT_ADMIN`; scoped + queue type department checks)
- `GET /kiosks/list` (role: `SUPERADMIN` or `DEPARTMENT_ADMIN`; filtered to in-scope departments)

- `POST /users/create` (permissions: `user_manage_global` or `user_manage_department`; scoped)
- `PUT /users/update` (permissions: `user_manage_global` or `user_manage_department`; scoped)
- `GET /users/list` (permissions: `user_manage_global` or `user_manage_department`; scoped filtering)
- `GET /users/department/{departmentId}` (permissions: `user_manage_global` or `user_manage_department`; scoped)
- `GET /users/{id}` (permissions: `user_manage_global` or `user_manage_department`; scoped)

- `POST /handlers/create` (permission: `handler_manage`; scoped)
- `PUT /handlers/update` (permission: `handler_manage`; scoped)
- `POST /handlers/start-session` (permission: `handler_manage`; scoped)
- `POST /handlers/end-session` (permission: `handler_manage`; scoped)
- `GET /handlers/list/{departmentId}` (permission: `handler_manage`; scoped)

## Company + Transaction Configuration
- `GET /companies/kiosk`
- `GET /companies/list`
- `GET /companies/{id}`
- `POST /companies/create`
- `PUT /companies/update/{id}`
- `DELETE /companies/deactivate/{id}`

- `GET /company-transactions/kiosk/company/{companyId}`
- `GET /company-transactions/company/{companyId}`
- `GET /company-transactions/{id}`
- `POST /company-transactions/create`
- `PUT /company-transactions/update/{id}`
- `DELETE /company-transactions/deactivate/{id}`

- `GET /company-transaction-destinations/kiosk/company-transaction/{companyTransactionId}`
- `GET /company-transaction-destinations/company-transaction/{companyTransactionId}`
- `GET /company-transaction-destinations/{id}`
- `POST /company-transaction-destinations/create`
- `PUT /company-transaction-destinations/update/{id}`
- `DELETE /company-transaction-destinations/deactivate/{id}`

## Ticket Lifecycle + Archive
- `POST /tickets/create`
- `GET /tickets/{ticketId}/printable`
- `GET /tickets/handler/context` (permission: `handler_call_next` or `supervisor_override`)
- `GET /tickets/handler/dashboard` (permission: `handler_call_next` or `supervisor_override`)
- `GET /tickets/handler/active-ticket` (permission: `handler_call_next` or `supervisor_override`)
- `GET /tickets/handler/history` (permission: `handler_call_next` or `supervisor_override`)
- `POST /tickets/handler/call-next` (permission: `handler_call_next`)
- `POST /tickets/handler/recall` (permission: `handler_recall`)
- `POST /tickets/handler/hold` (permission: `handler_hold`)
- `POST /tickets/handler/no-show` (permission: `handler_no_show`)
- `POST /tickets/handler/transfer` (permission: `handler_transfer`)
- `POST /tickets/handler/complete` (permission: `handler_complete`)
- `POST /tickets/cancel` (permission: `ticket_cancel` or `supervisor_override`)
- `POST /tickets/archive/day` (permission: `archive_manage`; scoped unless `report_view_global`)
- `GET /tickets/archived` (role constrained)
- `GET /tickets/live/{departmentId}` (role constrained)

## Display + Realtime
- `POST /displays/create` (permission: `display_manage`)
- `PUT /displays/update` (permission: `display_manage`)
- `POST /displays/assign-windows` (permission: `display_manage`)
- `GET /displays/list` (permission: `display_view`)
- `GET /displays/snapshot/{displayId}` (permission: `display_view` + display scope)
- `GET /displays/aggregate/{displayId}` (permission: `display_view` + display scope)

- `WS /realtime/ws/handler/{handlerId}?windowId=`
- `WS /realtime/ws/display/{displayId}?department_id=&area_id=&floor_id=&company_id=`
- `WS /realtime/ws/admin`

## Workflow, Audit, Reporting, Crew Validation
- `POST /workflow-templates/create` (permission: `workflow_template_manage`)
- `PUT /workflow-templates/update` (permission: `workflow_template_manage`)
- `POST /workflow-templates/assign` (permission: `workflow_template_assign`)
- `POST /workflow-templates/toggle` (permission: `workflow_template_manage`)
- `GET /workflow-templates/list?include_inactive=` (permission: `workflow_template_view`)
- `GET /workflow-templates/active?department_id=&queue_type_id=&company_id=&company_transaction_id=&transaction_family=` (permission: `workflow_template_view`)

- `GET /audit/logs` (permission: `audit_view`; optional scoped `departmentId`)

- `GET /reports/department-summary/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/handler-performance/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/queue-volume/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/archived-queues?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/department-archived-summary?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/daily-archive-metrics?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)

- `POST /crew-validation/validate`

## Response Structure Convention
- List-style reads generally return `ListResponse(data, result)`.
- Create endpoints generally return `IdResponse(id, result)`.
- Update/toggle/deactivate endpoints generally return `GlobalCredentialResponse`.
- Ticket lifecycle transitions return `TicketLifecycleResponse`.
