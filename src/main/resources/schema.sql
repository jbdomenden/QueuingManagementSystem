CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS departments (
    id SERIAL PRIMARY KEY,
    code VARCHAR(50) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL CHECK (role IN ('SUPERADMIN', 'DEPARTMENT_ADMIN', 'HANDLER')),
    department_id INT NULL REFERENCES departments(id),
    auth_token VARCHAR(255) NOT NULL UNIQUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);



CREATE TABLE IF NOT EXISTS user_department_scopes (
    user_id INT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    department_id INT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, department_id)
);

CREATE TABLE IF NOT EXISTS areas (
    id SERIAL PRIMARY KEY,
    department_id INT NOT NULL REFERENCES departments(id),
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS windows (
    id SERIAL PRIMARY KEY,
    department_id INT NOT NULL REFERENCES departments(id),
    area_id INT NULL REFERENCES areas(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(department_id, code)
);

CREATE TABLE IF NOT EXISTS handlers (
    id SERIAL PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    department_id INT NOT NULL REFERENCES departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(user_id)
);

CREATE TABLE IF NOT EXISTS companies (
    id SERIAL PRIMARY KEY,
    company_code VARCHAR(50) NOT NULL UNIQUE,
    company_short_name VARCHAR(100) NOT NULL,
    company_full_name VARCHAR(255) NOT NULL,
    display_size VARCHAR(20) NOT NULL CHECK (display_size IN ('BIG', 'SMALL')),
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


CREATE TABLE IF NOT EXISTS queue_users (
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    role VARCHAR(50) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    company_id INT NULL REFERENCES companies(id),
    department_id INT NULL REFERENCES departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    force_password_change BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queue_users_email ON queue_users(email);


CREATE TABLE IF NOT EXISTS queue_devices (
    id SERIAL PRIMARY KEY,
    device_key VARCHAR(255) UNIQUE NOT NULL,
    device_name VARCHAR(255) NOT NULL,
    device_type VARCHAR(20) NOT NULL CHECK (device_type IN ('KIOSK', 'DISPLAY')),
    company_id INT NULL REFERENCES companies(id),
    department_id INT NULL REFERENCES departments(id),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_seen_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queue_devices_key ON queue_devices(device_key);

CREATE TABLE IF NOT EXISTS queue_types (
    id SERIAL PRIMARY KEY,
    department_id INT NOT NULL REFERENCES departments(id),
    company_id INT NULL REFERENCES companies(id),
    name VARCHAR(255) NOT NULL,
    code VARCHAR(50) NOT NULL,
    prefix VARCHAR(10) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(department_id, code)
);


CREATE TABLE IF NOT EXISTS company_transactions (
    id SERIAL PRIMARY KEY,
    company_id INT NOT NULL REFERENCES companies(id),
    transaction_code VARCHAR(100) NOT NULL,
    transaction_name VARCHAR(255) NOT NULL,
    transaction_subtitle VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    requires_crew_validation BOOLEAN NOT NULL DEFAULT FALSE,
    input_mode VARCHAR(20) NOT NULL DEFAULT 'NONE' CHECK (input_mode IN ('NONE', 'KEYPAD', 'RFID', 'BOTH'))
);


CREATE TABLE IF NOT EXISTS company_transaction_destinations (
    id SERIAL PRIMARY KEY,
    company_transaction_id INT NOT NULL REFERENCES company_transactions(id),
    destination_code VARCHAR(100) NOT NULL,
    destination_name VARCHAR(255) NOT NULL,
    destination_subtitle VARCHAR(255) NULL,
    queue_type_id INT NULL REFERENCES queue_types(id),
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS kiosks (
    id SERIAL PRIMARY KEY,
    department_id INT NOT NULL REFERENCES departments(id),
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS kiosk_queue_types (
    kiosk_id INT NOT NULL REFERENCES kiosks(id) ON DELETE CASCADE,
    queue_type_id INT NOT NULL REFERENCES queue_types(id) ON DELETE CASCADE,
    PRIMARY KEY (kiosk_id, queue_type_id)
);

CREATE TABLE IF NOT EXISTS window_queue_types (
    window_id INT NOT NULL REFERENCES windows(id) ON DELETE CASCADE,
    queue_type_id INT NOT NULL REFERENCES queue_types(id) ON DELETE CASCADE,
    PRIMARY KEY (window_id, queue_type_id)
);

CREATE TABLE IF NOT EXISTS display_boards (
    id SERIAL PRIMARY KEY,
    department_id INT NOT NULL REFERENCES departments(id),
    area_id INT NULL REFERENCES areas(id),
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(department_id, code)
);

CREATE TABLE IF NOT EXISTS display_board_windows (
    display_board_id INT NOT NULL REFERENCES display_boards(id) ON DELETE CASCADE,
    window_id INT NOT NULL REFERENCES windows(id) ON DELETE CASCADE,
    PRIMARY KEY (display_board_id, window_id)
);

CREATE TABLE IF NOT EXISTS handler_sessions (
    id SERIAL PRIMARY KEY,
    handler_id INT NOT NULL REFERENCES handlers(id),
    user_id INT NOT NULL REFERENCES users(id),
    window_id INT NOT NULL REFERENCES windows(id),
    login_time TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    logout_time TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ONLINE' CHECK (status IN ('ONLINE', 'OFFLINE')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS queue_daily_sequences (
    queue_type_id INT NOT NULL REFERENCES queue_types(id),
    sequence_date DATE NOT NULL,
    current_value INT NOT NULL,
    PRIMARY KEY (queue_type_id, sequence_date)
);

CREATE TABLE IF NOT EXISTS tickets (
    id SERIAL PRIMARY KEY,
    ticket_number VARCHAR(50) NOT NULL UNIQUE,
    department_id INT NOT NULL REFERENCES departments(id),
    queue_type_id INT NOT NULL REFERENCES queue_types(id),
    company_id INT NULL REFERENCES companies(id),
    company_transaction_id INT NULL REFERENCES company_transactions(id),
    destination_id INT NULL REFERENCES company_transaction_destinations(id),
    crew_identifier VARCHAR(100) NULL,
    crew_identifier_type VARCHAR(20) NULL,
    crew_name VARCHAR(255) NULL,
    kiosk_id INT NULL REFERENCES kiosks(id),
    assigned_window_id INT NULL REFERENCES windows(id),
    assigned_handler_id INT NULL REFERENCES handlers(id),
    status VARCHAR(50) NOT NULL CHECK (status IN ('WAITING', 'CALLED', 'IN_SERVICE', 'HOLD', 'SKIPPED', 'COMPLETED', 'CANCELLED', 'TRANSFERRED')),
    service_date DATE NOT NULL DEFAULT CURRENT_DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    called_at TIMESTAMP NULL,
    service_started_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,
    last_action_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS ticket_logs (
    id SERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(id),
    action VARCHAR(50) NOT NULL,
    actor_handler_id INT NULL REFERENCES handlers(id),
    payload_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id SERIAL PRIMARY KEY,
    actor_user_id INT NULL REFERENCES users(id),
    department_id INT NULL REFERENCES departments(id),
    action VARCHAR(100) NOT NULL,
    entity_name VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    payload_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tickets_archived ON tickets(archived);
CREATE INDEX IF NOT EXISTS idx_tickets_service_date ON tickets(service_date);
CREATE INDEX IF NOT EXISTS idx_tickets_department_id ON tickets(department_id);
CREATE INDEX IF NOT EXISTS idx_tickets_status ON tickets(status);
CREATE INDEX IF NOT EXISTS idx_tickets_queue_type_id ON tickets(queue_type_id);
CREATE INDEX IF NOT EXISTS idx_tickets_assigned_window_id ON tickets(assigned_window_id);
CREATE INDEX IF NOT EXISTS idx_tickets_created_at ON tickets(created_at);
CREATE INDEX IF NOT EXISTS idx_tickets_department_status_created_at ON tickets(department_id, status, created_at);
CREATE INDEX IF NOT EXISTS idx_tickets_window_status_updated ON tickets(assigned_window_id, status, updated_at DESC) WHERE archived = false;
CREATE INDEX IF NOT EXISTS idx_tickets_queue_status_company ON tickets(queue_type_id, status, company_id) WHERE archived = false;

CREATE INDEX IF NOT EXISTS idx_handler_sessions_window_id ON handler_sessions(window_id);
CREATE INDEX IF NOT EXISTS idx_handler_sessions_status ON handler_sessions(status);
CREATE INDEX IF NOT EXISTS idx_handler_sessions_handler_active ON handler_sessions(handler_id, is_active);

CREATE INDEX IF NOT EXISTS idx_display_board_windows_display ON display_board_windows(display_board_id);
CREATE INDEX IF NOT EXISTS idx_window_queue_types_window_id ON window_queue_types(window_id);
CREATE INDEX IF NOT EXISTS idx_window_queue_types_queue_type ON window_queue_types(queue_type_id);


ALTER TABLE queue_types ADD COLUMN IF NOT EXISTS company_id INT NULL REFERENCES companies(id);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS company_transaction_id INT NULL REFERENCES company_transactions(id);

ALTER TABLE company_transactions ADD COLUMN IF NOT EXISTS requires_crew_validation BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE company_transactions ADD COLUMN IF NOT EXISTS input_mode VARCHAR(20) NOT NULL DEFAULT 'NONE';

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS company_id INT NULL REFERENCES companies(id);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS destination_id INT NULL REFERENCES company_transaction_destinations(id);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS crew_identifier VARCHAR(100) NULL;

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS crew_identifier_type VARCHAR(20) NULL;

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS crew_name VARCHAR(255) NULL;

CREATE TABLE IF NOT EXISTS user_sessions (
    session_id UUID PRIMARY KEY,
    user_id INT NOT NULL REFERENCES users(id),
    token_ref_hash VARCHAR(128) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'REVOKED', 'EXPIRED', 'LOGGED_OUT')),
    login_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_seen_at TIMESTAMP NOT NULL DEFAULT NOW(),
    logout_at TIMESTAMP NULL,
    ip_address VARCHAR(100) NULL,
    user_agent TEXT NULL,
    client_identifier VARCHAR(255) NULL,
    revoked_reason VARCHAR(100) NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_sessions_user_status ON user_sessions(user_id, status);
CREATE INDEX IF NOT EXISTS idx_user_sessions_last_seen ON user_sessions(last_seen_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_user_sessions_token_ref_hash ON user_sessions(token_ref_hash);

ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS client_identifier VARCHAR(255) NULL;
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS token_ref_hash VARCHAR(128);
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS revoked_reason VARCHAR(100) NULL;
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE';
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS login_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS last_seen_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE user_sessions ADD COLUMN IF NOT EXISTS logout_at TIMESTAMP NULL;
ALTER TABLE user_sessions ALTER COLUMN status TYPE VARCHAR(20);

INSERT INTO permissions(code, description)
VALUES
    ('session_view_self', 'View own sessions'),
    ('session_view_all', 'View all sessions in allowed scope'),
    ('session_revoke_self_other', 'Revoke own other sessions'),
    ('session_revoke_any', 'Revoke any session in allowed scope')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(code, description)
VALUES
    ('display_view', 'View display snapshots and aggregates'),
    ('display_manage', 'Create/update/assign display boards'),
    ('display_scope_department', 'Display access scoped to own department'),
    ('display_scope_global', 'Display access across all departments')
ON CONFLICT (code) DO NOTHING;



CREATE TABLE IF NOT EXISTS workflow_templates (
    id SERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    transaction_family VARCHAR(100) NULL,
    config_json JSONB NULL DEFAULT '{}'::jsonb,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by INT NULL REFERENCES users(id),
    updated_by INT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_steps (
    id SERIAL PRIMARY KEY,
    template_id INT NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    step_code VARCHAR(100) NOT NULL,
    step_name VARCHAR(255) NOT NULL,
    step_order INT NOT NULL DEFAULT 0,
    status_on_enter VARCHAR(50) NULL,
    status_on_exit VARCHAR(50) NULL,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    config_json JSONB NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(template_id, step_code)
);

CREATE TABLE IF NOT EXISTS workflow_department_assignments (
    id SERIAL PRIMARY KEY,
    template_id INT NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    department_id INT NOT NULL REFERENCES departments(id) ON DELETE CASCADE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE(template_id, department_id)
);

CREATE TABLE IF NOT EXISTS workflow_transaction_bindings (
    id SERIAL PRIMARY KEY,
    template_id INT NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    department_id INT NULL REFERENCES departments(id) ON DELETE CASCADE,
    company_id INT NULL REFERENCES companies(id) ON DELETE CASCADE,
    queue_type_id INT NULL REFERENCES queue_types(id) ON DELETE CASCADE,
    company_transaction_id INT NULL REFERENCES company_transactions(id) ON DELETE CASCADE,
    transaction_family VARCHAR(100) NULL,
    priority INT NOT NULL DEFAULT 0,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_by INT NULL REFERENCES users(id),
    updated_by INT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_status_rules (
    id SERIAL PRIMARY KEY,
    template_id INT NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    is_allowed BOOLEAN NOT NULL DEFAULT TRUE,
    reason_required BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS workflow_template_audit_logs (
    id BIGSERIAL PRIMARY KEY,
    template_id INT NOT NULL REFERENCES workflow_templates(id) ON DELETE CASCADE,
    actor_user_id INT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    payload_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_bindings_match ON workflow_transaction_bindings(department_id, queue_type_id, company_id, company_transaction_id, transaction_family, is_active);
CREATE INDEX IF NOT EXISTS idx_workflow_template_active ON workflow_templates(is_active);
CREATE INDEX IF NOT EXISTS idx_workflow_department_assignments_dept ON workflow_department_assignments(department_id, is_active);

ALTER TABLE tickets ADD COLUMN IF NOT EXISTS transaction_family VARCHAR(100) NULL;
ALTER TABLE tickets ADD COLUMN IF NOT EXISTS workflow_template_id INT NULL REFERENCES workflow_templates(id);

INSERT INTO permissions(code, description)
VALUES
    ('workflow_template_manage', 'Create/update/enable workflow templates'),
    ('workflow_template_view', 'View workflow templates and active bindings'),
    ('workflow_template_assign', 'Assign workflow templates to departments/transactions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO workflow_templates(code, name, description, transaction_family, config_json, is_active, created_at, updated_at)
VALUES
    ('FLIGHT_CANCELLATION', 'Flight Cancellation', 'Configurable workflow template migrated from legacy special workflow', 'FLIGHT_CANCELLATION', '{"legacy_workflow":"Flight Cancellation"}'::jsonb, true, NOW(), NOW()),
    ('OEC_MONITORING', 'OEC Monitoring', 'Configurable workflow template migrated from legacy special workflow', 'OEC_MONITORING', '{"legacy_workflow":"OEC Monitoring"}'::jsonb, true, NOW(), NOW()),
    ('OWWA_MONITORING', 'OWWA Monitoring', 'Configurable workflow template migrated from legacy special workflow', 'OWWA_MONITORING', '{"legacy_workflow":"OWWA Monitoring"}'::jsonb, true, NOW(), NOW()),
    ('WORKING_GEARS', 'Working Gears', 'Configurable workflow template migrated from legacy special workflow', 'WORKING_GEARS', '{"legacy_workflow":"Working Gears"}'::jsonb, true, NOW(), NOW())
ON CONFLICT (code) DO NOTHING;

INSERT INTO workflow_transaction_bindings(template_id, transaction_family, priority, is_active, created_at, updated_at)
SELECT id, transaction_family, 100, true, NOW(), NOW()
FROM workflow_templates
WHERE code IN ('FLIGHT_CANCELLATION', 'OEC_MONITORING', 'OWWA_MONITORING', 'WORKING_GEARS')
ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS queue_status_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    from_status VARCHAR(50) NOT NULL,
    to_status VARCHAR(50) NOT NULL,
    actor_user_id INT NULL REFERENCES users(id),
    actor_handler_id INT NULL REFERENCES handlers(id),
    reason TEXT NULL,
    metadata_json TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ticket_transfers (
    id BIGSERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    from_queue_type_id INT NULL REFERENCES queue_types(id),
    to_queue_type_id INT NULL REFERENCES queue_types(id),
    from_department_id INT NULL REFERENCES departments(id),
    to_department_id INT NULL REFERENCES departments(id),
    from_window_id INT NULL REFERENCES windows(id),
    to_window_id INT NULL REFERENCES windows(id),
    from_company_transaction_id INT NULL REFERENCES company_transactions(id),
    to_company_transaction_id INT NULL REFERENCES company_transactions(id),
    actor_user_id INT NULL REFERENCES users(id),
    actor_handler_id INT NULL REFERENCES handlers(id),
    reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS ticket_assignment_history (
    id BIGSERIAL PRIMARY KEY,
    ticket_id INT NOT NULL REFERENCES tickets(id) ON DELETE CASCADE,
    from_handler_id INT NULL REFERENCES handlers(id),
    to_handler_id INT NULL REFERENCES handlers(id),
    from_window_id INT NULL REFERENCES windows(id),
    to_window_id INT NULL REFERENCES windows(id),
    actor_user_id INT NULL REFERENCES users(id),
    actor_handler_id INT NULL REFERENCES handlers(id),
    reason TEXT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_queue_status_history_ticket ON queue_status_history(ticket_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ticket_transfers_ticket ON ticket_transfers(ticket_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_ticket_assignment_history_ticket ON ticket_assignment_history(ticket_id, created_at DESC);

CREATE TABLE IF NOT EXISTS daily_queue_archive (
    id BIGSERIAL PRIMARY KEY,
    archive_date DATE NOT NULL,
    department_id INT NOT NULL REFERENCES departments(id),
    queue_type_id INT NOT NULL REFERENCES queue_types(id),
    company_id INT NULL REFERENCES companies(id),
    handler_id INT NULL REFERENCES handlers(id),
    waiting_count INT NOT NULL DEFAULT 0,
    called_count INT NOT NULL DEFAULT 0,
    in_service_count INT NOT NULL DEFAULT 0,
    hold_count INT NOT NULL DEFAULT 0,
    no_show_count INT NOT NULL DEFAULT 0,
    completed_count INT NOT NULL DEFAULT 0,
    cancelled_count INT NOT NULL DEFAULT 0,
    transferred_count INT NOT NULL DEFAULT 0,
    override_count INT NOT NULL DEFAULT 0,
    avg_waiting_seconds BIGINT NOT NULL DEFAULT 0,
    avg_serving_seconds BIGINT NOT NULL DEFAULT 0,
    source_ticket_count INT NOT NULL DEFAULT 0,
    archived_ticket_count INT NOT NULL DEFAULT 0,
    process_reference VARCHAR(100) NULL,
    archive_metadata_json TEXT NULL,
    created_by INT NULL REFERENCES users(id),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (archive_date, department_id, queue_type_id, company_id, handler_id)
);

CREATE INDEX IF NOT EXISTS idx_daily_queue_archive_date_scope ON daily_queue_archive(archive_date, department_id, queue_type_id);
CREATE INDEX IF NOT EXISTS idx_daily_queue_archive_department ON daily_queue_archive(department_id, archive_date DESC);
CREATE INDEX IF NOT EXISTS idx_daily_queue_archive_queue_type ON daily_queue_archive(queue_type_id, archive_date DESC);

INSERT INTO permissions(code, description)
VALUES
    ('handler_call_next', 'Handler can call next ticket'),
    ('handler_recall', 'Handler can recall active ticket'),
    ('handler_hold', 'Handler can hold active ticket'),
    ('handler_no_show', 'Handler can mark ticket as no show'),
    ('handler_transfer', 'Handler can transfer ticket'),
    ('handler_complete', 'Handler can complete ticket'),
    ('ticket_cancel', 'Can cancel ticket'),
    ('supervisor_override', 'Can override workflow restrictions')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(code, description)
VALUES
    ('user_manage_global', 'Manage users in all departments'),
    ('user_manage_department', 'Manage users inside assigned department scope'),
    ('department_manage', 'Manage departments'),
    ('handler_manage', 'Manage handlers inside assigned scope'),
    ('window_manage', 'Manage windows inside assigned scope'),
    ('queue_type_manage', 'Manage queue types inside assigned scope'),
    ('ticket_override', 'Override ticket workflow in allowed scope'),
    ('report_view_scope', 'View reports in allowed scope')
ON CONFLICT (code) DO NOTHING;

INSERT INTO permissions(code, description)
VALUES
    ('audit_view', 'View immutable audit trail logs'),
    ('report_view_department', 'View departmental reporting and archive metrics'),
    ('report_view_global', 'View global reporting and archive metrics'),
    ('archive_manage', 'Run end-of-day queue archival process')
ON CONFLICT (code) DO NOTHING;

-- Sample handler seed user for local development login
INSERT INTO departments(code, name, is_active)
VALUES ('SME', 'SME Department', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO users(username, password_hash, full_name, role, department_id, auth_token, is_active)
VALUES (
    'sme',
    crypt('sme', gen_salt('bf', 12)),
    'SME SUPPLIER',
    'HANDLER',
    (SELECT id FROM departments WHERE code = 'SME' LIMIT 1),
    encode(gen_random_bytes(32), 'hex'),
    true
)
ON CONFLICT (username) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    department_id = EXCLUDED.department_id,
    is_active = true;

INSERT INTO user_department_scopes(user_id, department_id)
SELECT u.id, d.id
FROM users u
JOIN departments d ON d.code = 'SME'
WHERE u.username = 'sme'
ON CONFLICT (user_id, department_id) DO NOTHING;

INSERT INTO handlers(user_id, department_id, is_active)
SELECT u.id, d.id, true
FROM users u
JOIN departments d ON d.code = 'SME'
WHERE u.username = 'sme'
ON CONFLICT (user_id) DO UPDATE SET
    department_id = EXCLUDED.department_id,
    is_active = true;

