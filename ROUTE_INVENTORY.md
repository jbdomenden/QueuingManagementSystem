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
