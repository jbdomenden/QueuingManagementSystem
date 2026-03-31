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
