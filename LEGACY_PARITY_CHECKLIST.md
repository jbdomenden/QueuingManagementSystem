# Legacy Parity Checklist

## Completed in this phase

- [x] Comprehensive immutable audit events now cover login/logout/failed login/password change/forced logout plus ticket lifecycle transitions (called, recalled, hold, no show, transfer, complete, cancel) and workflow template change events.
- [x] Scope-sensitive admin changes now write audit logs for users, handlers, windows, window queue assignments, queue types, and departments.
- [x] Added durable reporting-oriented history/summary storage via `daily_queue_archive`, extended from existing end-of-day archive flow (`POST /tickets/archive/day`) instead of replacing it.
- [x] Reporting now includes archive-derived daily metrics for department/queue type/status dimensions with average waiting/serving times plus transfer/no-show/completed/cancelled and override counts when derivable from persisted audit actions.
- [x] Added permissions for `audit_view`, `report_view_department`, `report_view_global`, and `archive_manage`.

## Final stabilization status

### completed
- Unified password policy enforcement for both `POST /auth/change-password` and `POST /users/create` validation paths.
- Normalized kiosk admin route protection so all kiosk write/list endpoints are authenticated and department scoped.
- Added scope validation for kiosk queue-type assignment to prevent cross-department mappings.
- Replaced interpolated ticket-log metadata JSON with structured JSON serialization while keeping prepared-statement parameterization.
- Reconciled route documentation with currently registered backend routes and permission/scope expectations.

### partially completed
- Response envelope conventions are largely consistent (`GlobalCredentialResponse`, `IdResponse`, `ListResponse`), with a few intentional legacy direct-model responses preserved for compatibility.
- Some role-gated legacy endpoints still coexist with newer permission-based flows; behavior is coherent but not yet fully converged to one authorization model.

### intentionally deferred
- Full refactor of all route files into a single shared authorization DSL was deferred to avoid endpoint-behavior risk during stabilization.
- Websocket authentication model harmonization across handler/admin sockets was deferred because existing clients may rely on current handshake behavior.

### notes and rationale
- This phase prioritized strict integration hardening (scope/permission coherence, password policy strength, parameterized persistence safety, and docs parity) without expanding feature scope.
- Changes were kept local to existing modules to avoid architectural churn and preserve currently working endpoints.
