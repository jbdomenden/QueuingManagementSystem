# Migration Map

## Legacy LCD / Queue Monitoring Behavior Mapping

### Legacy behavior: currently called ticket per window
- Implemented by `DisplayAggregationService.getDisplayAggregateSnapshot` using `getDisplayCurrentCalledTicketsQuery`, returning one active `CALLED` ticket per mapped window in `current_called_tickets`.
- Exposed via `GET /displays/aggregate/{displayId}` and pushed over `/realtime/ws/display/{displayId}` events.

### Legacy behavior: currently serving ticket per window
- Implemented by `DisplayAggregationService.getDisplayAggregateSnapshot` using `getDisplayCurrentServingTicketsQuery`, returned as `current_serving_tickets`.
- Includes only `IN_SERVICE` records for windows assigned to the display board.

### Legacy behavior: waiting counts by queue type
- Implemented by `getDisplayWaitingCountsByQueueTypeQuery` and returned as `waiting_counts_by_queue_type`.
- Count source is committed `WAITING` tickets associated to queue types eligible for mapped display windows.

### Legacy behavior: hold count and no-show count
- Implemented by status aggregation query (`getDisplayCountsByStatusQuery`).
- `hold_count` is computed from `HOLD` status count.
- `no_show_count` is computed from `SKIPPED` status count.

### Legacy behavior: special/visitor count
- `visitor_count` is implemented via `getDisplayVisitorCountQuery` (`company_id IS NULL` within scoped queue types).
- `special_count` is reserved and currently `null` because no dedicated special-ticket flag exists in the current schema.

### Legacy behavior: filtering by area, department, floor, company
- Department, area, and company filters are available on display aggregate REST and websocket subscriptions.
- Floor filter input is accepted for compatibility (`floor_id`) and reported as unsupported (`floor_filter_supported = false`) because the current data model has no floor column.

### Legacy behavior: real-time display updates without polling
- Existing display websocket path is preserved and extended with scoped aggregate payload streaming.
- `EventPublisher` now broadcasts aggregate snapshots per websocket subscription filter when ticket lifecycle events are committed.

### Commit-bound aggregation guarantee
- Ticket lifecycle mutations are committed before realtime publish calls are triggered by routes/controllers.
- Display aggregation queries read committed database state only, ensuring LCD payloads reflect committed transitions.

## Legacy Special Workflow Migration (Template-Driven)

### Legacy workflow: Flight Cancellation
- Mapped to seeded workflow template code `FLIGHT_CANCELLATION` in `workflow_templates`.
- Bound through `workflow_transaction_bindings.transaction_family = 'FLIGHT_CANCELLATION'` so it is selected by active workflow resolution without hardcoded page logic.

### Legacy workflow: OEC Monitoring
- Mapped to seeded workflow template code `OEC_MONITORING`.
- Activated through configurable binding records and optional department/queue/company/transaction scoping.

### Legacy workflow: OWWA Monitoring
- Mapped to seeded workflow template code `OWWA_MONITORING`.
- Uses the same generic workflow template engine and binding model (no custom module duplication).

### Legacy workflow: Working Gears
- Mapped to seeded workflow template code `WORKING_GEARS`.
- Retrieved by generic active workflow lookup and associated to tickets via `tickets.workflow_template_id`.

### Generic extensibility path
- Additional special workflows can be onboarded by creating rows in `workflow_templates`, defining `workflow_steps` and `workflow_status_rules`, then assigning through `workflow_transaction_bindings` and/or `workflow_department_assignments`.
- Ticket creation now resolves active workflow bindings and stores selected template id on the ticket, preserving generic lifecycle behavior while enabling template-specific orchestration.
