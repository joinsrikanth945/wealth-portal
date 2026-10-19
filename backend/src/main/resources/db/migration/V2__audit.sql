-- Audit trail: insert-only record of who did what.

CREATE TABLE audit_event (
    id           UUID PRIMARY KEY,
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    actor        VARCHAR(255) NOT NULL,
    action       VARCHAR(64) NOT NULL,
    detail       VARCHAR(2000)
);

CREATE INDEX idx_audit_event_time ON audit_event (occurred_at DESC);
CREATE INDEX idx_audit_event_actor ON audit_event (actor, occurred_at DESC);

-- Belt and braces: the database itself refuses edits and deletes.
CREATE FUNCTION audit_event_is_append_only() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'audit_event rows cannot be changed or deleted';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER audit_event_no_update_delete
    BEFORE UPDATE OR DELETE ON audit_event
    FOR EACH ROW EXECUTE FUNCTION audit_event_is_append_only();
