CREATE TABLE activity_feed (
    id          UUID        PRIMARY KEY,
    user_id     UUID        NOT NULL,
    app_id      UUID        NOT NULL,
    event_type  VARCHAR(30) NOT NULL,
    message     TEXT        NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_feed_app_occurred  ON activity_feed (app_id,  occurred_at DESC);
CREATE INDEX idx_feed_user_occurred ON activity_feed (user_id, occurred_at DESC);

ALTER TABLE activity_feed
    ADD CONSTRAINT uq_feed_app_event_occurred
    UNIQUE (app_id, event_type, occurred_at);
