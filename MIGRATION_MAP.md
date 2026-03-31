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
