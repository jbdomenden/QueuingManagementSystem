# ARCHITECTURE BASELINE

## Scope
This baseline captures the current Ktor API surface and access model without changing runtime behavior.

### Package map (current)
- `QueuingManagementSystem.controllers`: endpoint orchestration + authorization checks per route use case.
- `QueuingManagementSystem.models`: request/response DTOs, validation helpers, and shared payload types.
- `QueuingManagementSystem.queries`: SQL/data access layer wrappers used by controllers.
- `QueuingManagementSystem.common`: auth token extraction, role/scope helper functions, permission guards, and common request metadata helpers.
- `QueuingManagementSystem.config`: connection pool and bootstrap/seeding configuration.

### Controllers observed
- Required set present: `AuthController`, `SessionController`, `UserController`, `HandlerController`, `KioskController`, `DisplayController`, `TicketController`, `AuditController`, `ReportController`, `CompanyController`, `DepartmentController`, `WindowController`, `QueueTypeController`, `WorkflowTemplateController`.
- Additional controllers currently in repo: `AreaController`, `CompanyTransactionController`, `CompanyTransactionDestinationController`, `CrewValidationController`.

## Route categories

### Staff endpoints (authenticated admin/staff workflows)
- `POST /auth/logout`
- `GET /auth/validate`
- `POST /auth/change-password`
- `GET /auth/me`
- `GET /sessions/me`
- `GET /sessions/admin`
- `POST /sessions/me/revoke-others`
- `POST /sessions/{sessionId}/revoke`
- `POST /users/create`
- `PUT /users/update`
- `GET /users/list`
- `GET /users/department/{departmentId}`
- `GET /users/{id}`
- `POST /handlers/create`
- `PUT /handlers/update`
- `POST /handlers/start-session`
- `POST /handlers/end-session`
- `GET /handlers/list/{departmentId}`
- `POST /kiosks/create`
- `PUT /kiosks/update`
- `POST /kiosks/assign-queue-types`
- `GET /kiosks/list`
- `POST /displays/create`
- `PUT /displays/update`
- `POST /displays/assign-windows`
- `GET /displays/list`
- `GET /tickets/handler/context`
- `GET /tickets/handler/dashboard`
- `GET /tickets/handler/active-ticket`
- `GET /tickets/handler/history`
- `GET /tickets/handler/queue`
- `POST /tickets/handler/call`
- `POST /tickets/handler/call-next`
- `POST /tickets/handler/start-service`
- `POST /tickets/handler/recall`
- `POST /tickets/handler/hold`
- `POST /tickets/handler/no-show`
- `POST /tickets/handler/transfer`
- `POST /tickets/handler/complete`
- `POST /tickets/cancel`
- `POST /tickets/archive/day`
- `GET /tickets/archived`
- `GET /tickets/live/{departmentId}`
- `POST /departments/create`
- `PUT /departments/update`
- `GET /departments/list`
- `POST /areas/create`
- `PUT /areas/update`
- `GET /areas/list/{departmentId}`
- `POST /windows/create`
- `PUT /windows/update`
- `GET /windows/list/{departmentId}`
- `POST /windows/assign-queue-types`
- `POST /queue-types/create`
- `PUT /queue-types/update`
- `GET /queue-types/list/{departmentId}`
- `GET /companies/list`
- `GET /companies/{id}`
- `POST /companies/create`
- `PUT /companies/update/{id}`
- `DELETE /companies/deactivate/{id}`
- `DELETE /companies/{id}`
- `GET /company-transactions/company/{companyId}`
- `GET /company-transactions/{id}`
- `POST /company-transactions/create`
- `PUT /company-transactions/update/{id}`
- `PATCH /company-transactions/toggle/{id}`
- `DELETE /company-transactions/deactivate/{id}`
- `GET /company-transaction-destinations/company-transaction/{companyTransactionId}`
- `GET /company-transaction-destinations/{id}`
- `POST /company-transaction-destinations/create`
- `PUT /company-transaction-destinations/update/{id}`
- `PATCH /company-transaction-destinations/toggle/{id}`
- `DELETE /company-transaction-destinations/deactivate/{id}`
- `POST /workflow-templates/create`
- `PUT /workflow-templates/update`
- `POST /workflow-templates/assign`
- `POST /workflow-templates/toggle`
- `GET /workflow-templates/list`
- `GET /workflow-templates/active`
- `GET /reports/department-summary/{departmentId}`
- `GET /reports/handler-performance/{departmentId}`
- `GET /reports/queue-volume/{departmentId}`
- `GET /reports/archived-queues`
- `GET /reports/department-archived-summary`
- `GET /reports/daily-archive-metrics`
- `GET /audit/logs`

### Kiosk endpoints (public device-facing queue intake)
- `GET /companies/kiosk`
- `GET /company-transactions/kiosk/company/{companyId}`
- `GET /company-transaction-destinations/kiosk/company-transaction/{companyTransactionId}`
- `POST /tickets/create`

### Display endpoints (public display consumption)
- `GET /displays/wallboard/{displayId}`
- `GET /displays/snapshot/{displayId}`
- `GET /displays/aggregate/{displayId}`
- `GET /tickets/{ticketId}/printable`
- `GET /realtime/ws/display/{displayId}` (WebSocket)

### Mixed / weakly protected endpoints
- `POST /auth/login` (credential-based public login entry point)
- `POST /crew-validation/validate` (no auth)
- `GET /queue-types/company/{companyId}` (no auth)
- `GET /realtime/ws/handler/{handlerId}?windowId=...` (no token check; id-based)
- `GET /realtime/ws/admin` (no token check)
- `GET /displays/wallboard/{displayId}`, `/snapshot/{displayId}`, `/aggregate/{displayId}` (no token; active display check only)
- `GET /tickets/{ticketId}/printable` (no token)

## Current auth gaps
- Inconsistent session APIs are used across routes (`getValidatedSessionByToken` vs `getUserSessionByToken`), producing uneven null/expired-token handling and uneven scope normalization.
- Some routes enforce permissions only, others enforce role-only, and some require both; this is not centralized and leads to divergent protections for similar resources.
- Several public endpoints are intentionally open (kiosk/display/printable), but there is no explicit allowlist policy document in code to distinguish intentional openness from missing auth.
- WebSocket routes under `/realtime` currently rely on path/query parameters and active-display checks only; they do not validate bearer tokens.

## Current role handling gaps
- Role checks are string-comparison based and vary by route (`SUPERADMIN`, `DEPARTMENT_ADMIN`, and helper variants in `ScopeCommon`).
- Some routes use permissions as primary authorization, while others use role gates first and skip granular permissions.
- Department scoping is applied in many write/list routes, but not uniformly for all role-gated endpoints.
- Department admin behavior differs by module (strict department equality in some modules vs permission+scope combinations in others).

## Current device access gaps
- Kiosk/data discovery endpoints are anonymous; there is no kiosk device credential binding in route layer.
- Display polling/aggregation and display websocket are anonymous if display is active.
- Handler websocket and admin websocket lack route-level authentication.
- `X-Client-Id` is captured on login but not consistently used as a required credential for kiosk/display socket access paths.

## Notes on behavior preservation baseline
- Queue creation/calling/status transitions, assignment flows, kiosk flow, display aggregation, archive logic, handler lifecycle, and workflow routing were intentionally not modified in this baseline update.
