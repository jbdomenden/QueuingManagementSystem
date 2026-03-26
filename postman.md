# Postman Collection Guide - Queuing Management System

Base URL: `http://localhost:9000`

## 1) Authentication

### POST `/auth/login`
**Body**
```json
{
  "username": "superadmin",
  "password": "password123"
}
```
**Sample Response**
```json
{
  "user_id": 1,
  "full_name": "System Superadmin",
  "role": "SUPERADMIN",
  "department_id": null,
  "token": "7d6416c5-f9fb-45dd-a80f-c70f1658879f",
  "result": { "Code": 200, "Access": true, "Status": "Login successful" }
}
```

### GET `/auth/me`
**Headers**
- `Authorization: Bearer <token>`

**Sample Response**
```json
{
  "user_id": 1,
  "department_id": null,
  "role": "SUPERADMIN",
  "token": "7d6416c5-f9fb-45dd-a80f-c70f1658879f"
}
```

---

## 2) Departments

### POST `/departments/create`
```json
{
  "code": "REG",
  "name": "Registrar",
  "is_active": true
}
```

### PUT `/departments/update`
```json
{
  "id": 1,
  "code": "REG",
  "name": "Registrar Office",
  "is_active": true
}
```

### GET `/departments/list`
No body.

---

## 3) Users

### POST `/users/create`
```json
{
  "username": "reg_admin",
  "password": "password123",
  "full_name": "Registrar Admin",
  "role": "DEPARTMENT_ADMIN",
  "department_id": 1,
  "is_active": true
}
```

### PUT `/users/update`
```json
{
  "id": 2,
  "username": "reg_admin",
  "full_name": "Registrar Admin Updated",
  "role": "DEPARTMENT_ADMIN",
  "department_id": 1,
  "is_active": true
}
```

### GET `/users/list`
No body.

---

## 4) Areas

### Route Group `/areas`
`AreaRoutes` is scaffolded for:
- create
- update
- list by department

(Implementation placeholder currently.)

---

## 5) Windows

### POST `/windows/create`
```json
{
  "department_id": 1,
  "area_id": 1,
  "code": "W1",
  "name": "Window 1",
  "is_active": true
}
```

### PUT `/windows/update`
```json
{
  "id": 1,
  "department_id": 1,
  "area_id": 1,
  "code": "W1",
  "name": "Window 1 - Updated",
  "is_active": true
}
```

### GET `/windows/list/{departmentId}`
Example: `/windows/list/1`

### POST `/windows/assign-queue-types`
```json
{
  "window_id": 1,
  "queue_type_ids": [1, 2]
}
```

---

## 6) Handlers

### POST `/handlers/create`
```json
{
  "user_id": 3,
  "department_id": 1,
  "is_active": true
}
```

### PUT `/handlers/update`
```json
{
  "id": 1,
  "user_id": 3,
  "department_id": 1,
  "is_active": true
}
```

### POST `/handlers/start-session`
```json
{
  "handler_id": 1,
  "window_id": 1
}
```

### POST `/handlers/end-session`
```json
{
  "handler_id": 1,
  "window_id": 1
}
```

### GET `/handlers/list/{departmentId}`
Example: `/handlers/list/1`

---

## 7) Queue Types

### POST `/queue-types/create`
```json
{
  "department_id": 1,
  "name": "Payments",
  "code": "PAY",
  "prefix": "PAY",
  "is_active": true
}
```

### PUT `/queue-types/update`
```json
{
  "id": 1,
  "department_id": 1,
  "name": "Payments Updated",
  "code": "PAY",
  "prefix": "PAY",
  "is_active": true
}
```

### GET `/queue-types/list/{departmentId}`
Example: `/queue-types/list/1`

---

## 8) Kiosks

### POST `/kiosks/create`
```json
{
  "department_id": 1,
  "name": "Main Lobby Kiosk",
  "is_active": true
}
```

### PUT `/kiosks/update`
```json
{
  "id": 1,
  "department_id": 1,
  "name": "Main Lobby Kiosk Updated",
  "is_active": true
}
```

### POST `/kiosks/assign-queue-types`
```json
{
  "kiosk_id": 1,
  "queue_type_ids": [1, 2]
}
```

### GET `/kiosks/list`
No body.

---

## 9) Displays

### POST `/displays/create`
```json
{
  "department_id": 1,
  "area_id": 1,
  "code": "D-A1",
  "name": "Area A Display",
  "is_active": true
}
```

### PUT `/displays/update`
```json
{
  "id": 1,
  "department_id": 1,
  "area_id": 1,
  "code": "D-A1",
  "name": "Area A Display Updated",
  "is_active": true
}
```

### POST `/displays/assign-windows`
```json
{
  "display_board_id": 1,
  "window_ids": [1, 2]
}
```

### GET `/displays/snapshot/{displayId}`
Example: `/displays/snapshot/1`

### GET `/displays/list`
No body.

---

## 10) Tickets

### POST `/tickets/create`
```json
{
  "kiosk_id": 1,
  "queue_type_id": 1
}
```
**Sample Response**
```json
{
  "id": 10,
  "ticket_number": "PAY-001",
  "department_id": 1,
  "queue_type_id": 1,
  "kiosk_id": 1,
  "assigned_window_id": null,
  "assigned_handler_id": null,
  "status": "WAITING",
  "created_at": "2026-03-26T10:10:00Z",
  "called_at": null,
  "completed_at": null
}
```

### GET `/tickets/live/{departmentId}`
Example: `/tickets/live/1`

### POST `/tickets/call-next`
```json
{
  "handler_id": 1
}
```

### POST `/tickets/start-service`
```json
{
  "handler_id": 1,
  "ticket_id": 10
}
```

### POST `/tickets/skip`
```json
{
  "handler_id": 1,
  "ticket_id": 10
}
```

### POST `/tickets/recall`
```json
{
  "handler_id": 1,
  "ticket_id": 10
}
```

### POST `/tickets/complete`
```json
{
  "handler_id": 1,
  "ticket_id": 10
}
```

---

## 11) Reports

### GET `/reports/department-summary/{departmentId}`
Example: `/reports/department-summary/1`

### GET `/reports/handler-performance/{departmentId}`
Example: `/reports/handler-performance/1`

### GET `/reports/queue-volume/{departmentId}`
Example: `/reports/queue-volume/1`

---

## 12) Audit

### GET `/audit/logs`
Optional query param:
- `departmentId`

Examples:
- `/audit/logs`
- `/audit/logs?departmentId=1`

---

## 13) WebSocket Channels

### Handler Channel
`/ws/handler/{handlerId}`

### Display Channel
`/ws/display/{displayId}`

### Admin Channel
`/ws/admin`

---

## Common Protected Headers
For protected endpoints include:
- `Authorization: Bearer <token>`
- `Content-Type: application/json`

## Common Success Envelope Example
```json
{
  "Code": 200,
  "Access": true,
  "Status": "OK"
}
```
