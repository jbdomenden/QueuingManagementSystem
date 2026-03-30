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

CREATE INDEX IF NOT EXISTS idx_handler_sessions_window_id ON handler_sessions(window_id);
CREATE INDEX IF NOT EXISTS idx_handler_sessions_status ON handler_sessions(status);
CREATE INDEX IF NOT EXISTS idx_handler_sessions_handler_active ON handler_sessions(handler_id, is_active);

CREATE INDEX IF NOT EXISTS idx_display_board_windows_display ON display_board_windows(display_board_id);
CREATE INDEX IF NOT EXISTS idx_window_queue_types_window_id ON window_queue_types(window_id);
CREATE INDEX IF NOT EXISTS idx_window_queue_types_queue_type ON window_queue_types(queue_type_id);


ALTER TABLE queue_types ADD COLUMN IF NOT EXISTS company_id INT NULL REFERENCES companies(id);
