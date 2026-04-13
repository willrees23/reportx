CREATE TABLE IF NOT EXISTS cases (
    id TEXT PRIMARY KEY,
    target_id TEXT NOT NULL,
    category TEXT NOT NULL,
    status TEXT NOT NULL,
    claimed_by TEXT,
    claimed_at INTEGER,
    resolved_by TEXT,
    resolved_at INTEGER,
    resolution_reason TEXT,
    created_at INTEGER NOT NULL,
    last_activity_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_cases_status_category_created
    ON cases (status, category, created_at);

CREATE INDEX IF NOT EXISTS idx_cases_target_category_status
    ON cases (target_id, category, status);

CREATE TABLE IF NOT EXISTS reports (
    id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    reporter_id TEXT NOT NULL,
    target_id TEXT NOT NULL,
    category TEXT NOT NULL,
    detail TEXT,
    server_name TEXT NOT NULL,
    reporter_coords TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (case_id) REFERENCES cases(id)
);

CREATE INDEX IF NOT EXISTS idx_reports_case ON reports (case_id);
CREATE INDEX IF NOT EXISTS idx_reports_target_category_created
    ON reports (target_id, category, created_at);

CREATE TABLE IF NOT EXISTS evidence (
    id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    label TEXT NOT NULL,
    content TEXT NOT NULL,
    author_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    edited_at INTEGER,
    FOREIGN KEY (case_id) REFERENCES cases(id)
);

CREATE INDEX IF NOT EXISTS idx_evidence_case ON evidence (case_id);

CREATE TABLE IF NOT EXISTS notes (
    id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    body TEXT NOT NULL,
    author_id TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    edited_at INTEGER,
    FOREIGN KEY (case_id) REFERENCES cases(id)
);

CREATE INDEX IF NOT EXISTS idx_notes_case ON notes (case_id);

CREATE TABLE IF NOT EXISTS audit_entries (
    id TEXT PRIMARY KEY,
    case_id TEXT NOT NULL,
    actor_id TEXT,
    event_type TEXT NOT NULL,
    payload TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    FOREIGN KEY (case_id) REFERENCES cases(id)
);

CREATE INDEX IF NOT EXISTS idx_audit_case_created ON audit_entries (case_id, created_at);

CREATE TABLE IF NOT EXISTS log_buffer (
    player_id TEXT NOT NULL,
    type TEXT NOT NULL,
    content TEXT NOT NULL,
    server_name TEXT NOT NULL,
    created_at INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_log_buffer_player_type_created
    ON log_buffer (player_id, type, created_at);

CREATE INDEX IF NOT EXISTS idx_log_buffer_type_created
    ON log_buffer (type, created_at);
