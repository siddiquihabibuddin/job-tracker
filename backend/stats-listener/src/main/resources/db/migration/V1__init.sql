CREATE TABLE applications_snapshot (
    id         UUID PRIMARY KEY,
    user_id    UUID NOT NULL,
    status     VARCHAR(20) NOT NULL,
    source     VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_snap_user_created ON applications_snapshot(user_id, created_at DESC);
