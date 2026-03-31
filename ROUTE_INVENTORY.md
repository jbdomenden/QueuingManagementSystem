# Route Inventory

## Auth Routes
- `POST /auth/login`
- `POST /auth/logout`
- `GET /auth/validate`
- `POST /auth/change-password`
- `GET /auth/me`

## Session Routes
- `GET /sessions/me` (permission: `session_view_self`)
- `GET /sessions/admin` (permission: `session_view_all`; scoped by actor role/department)
- `POST /sessions/me/revoke-others` (permission: `session_revoke_self_other`)
- `POST /sessions/{sessionId}/revoke` (permission: `session_revoke_any`; super admin or scoped admin)

## Scoped Admin & Operational Routes
- `POST /users/create` (permissions: `user_manage_global` or `user_manage_department`; enforces department scope)
- `PUT /users/update` (permissions: `user_manage_global` or `user_manage_department`; enforces department scope)
- `GET /users/list` (permissions: `user_manage_global` or `user_manage_department`; filters out out-of-scope users)
- `GET /users/department/{departmentId}` (permissions: `user_manage_global` or `user_manage_department`; department scope check)
- `GET /users/{id}` (permissions: `user_manage_global` or `user_manage_department`; scope check)
- `POST /handlers/create` (permission: `handler_manage`; scope check)
- `PUT /handlers/update` (permission: `handler_manage`; scope check)
- `GET /handlers/list/{departmentId}` (permission: `handler_manage`; scope check)
- `POST /windows/create` (permission: `window_manage`; scope check)
- `PUT /windows/update` (permission: `window_manage`; server-side scope lookup)
- `GET /windows/list/{departmentId}` (permission: `window_manage`; scope check)
- `POST /windows/assign-queue-types` (permission: `window_manage`; validates window and queue type scope)
- `POST /queue-types/create` (permission: `queue_type_manage`; scope check)
- `PUT /queue-types/update` (permission: `queue_type_manage`; server-side scope lookup)
- `GET /queue-types/list/{departmentId}` (permission: `queue_type_manage`; scope check)

## Ticket Lifecycle Routes
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
- `POST /tickets/archive/day` (permission: `archive_manage`; scoped by actor department scope unless global report scope is present)
- `GET /tickets/archived`
- `GET /tickets/live/{departmentId}`

## Display Routes
- `POST /displays/create` (permission: `display_manage`; role scoped to admin/superadmin)
- `PUT /displays/update` (permission: `display_manage`; role scoped to admin/superadmin)
- `POST /displays/assign-windows` (permission: `display_manage`; role scoped to admin/superadmin)
- `GET /displays/list` (permission: `display_view`)
- `GET /displays/snapshot/{displayId}` (permission: `display_view` + `display_scope_department|display_scope_global`)
- `GET /displays/aggregate/{displayId}?department_id=&area_id=&floor_id=&company_id=` (permission: `display_view` + `display_scope_department|display_scope_global`)

## Realtime Routes
- `GET /realtime/ws/handler/{handlerId}?windowId=` (websocket)
- `GET /realtime/ws/display/{displayId}?department_id=&area_id=&floor_id=&company_id=` (websocket, permission: `display_view` + display scope)
- `GET /realtime/ws/admin` (websocket)

## Workflow Template Routes
- `POST /workflow-templates/create` (permission: `workflow_template_manage`)
- `PUT /workflow-templates/update` (permission: `workflow_template_manage`)
- `POST /workflow-templates/assign` (permission: `workflow_template_assign`)
- `POST /workflow-templates/toggle` (permission: `workflow_template_manage`)
- `GET /workflow-templates/list?include_inactive=` (permission: `workflow_template_view`)
- `GET /workflow-templates/active?department_id=&queue_type_id=&company_id=&company_transaction_id=&transaction_family=` (permission: `workflow_template_view`)

## Audit & Report Routes
- `GET /audit/logs` (permission: `audit_view`; optional `departmentId` scope filter)
- `GET /reports/department-summary/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/handler-performance/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/queue-volume/{departmentId}` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/archived-queues?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/department-archived-summary?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)
- `GET /reports/daily-archive-metrics?dateFrom=&dateTo=&departmentId=` (permission: `report_view_department` or `report_view_global`)
