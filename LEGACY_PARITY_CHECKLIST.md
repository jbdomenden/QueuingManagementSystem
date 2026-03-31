# Legacy Parity Checklist

## Completed in this phase

- [x] Comprehensive immutable audit events now cover login/logout/failed login/password change/forced logout plus ticket lifecycle transitions (called, recalled, hold, no show, transfer, complete, cancel) and workflow template change events.
- [x] Scope-sensitive admin changes now write audit logs for users, handlers, windows, window queue assignments, queue types, and departments.
- [x] Added durable reporting-oriented history/summary storage via `daily_queue_archive`, extended from existing end-of-day archive flow (`POST /tickets/archive/day`) instead of replacing it.
- [x] Reporting now includes archive-derived daily metrics for department/queue type/status dimensions with average waiting/serving times plus transfer/no-show/completed/cancelled and override counts when derivable from persisted audit actions.
- [x] Added permissions for `audit_view`, `report_view_department`, `report_view_global`, and `archive_manage`.
