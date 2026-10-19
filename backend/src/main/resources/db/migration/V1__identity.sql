-- Identity: our link to Keycloak accounts, plus sign-in history.

CREATE TABLE app_user (
    id             UUID PRIMARY KEY,
    keycloak_id    VARCHAR(255) NOT NULL UNIQUE,
    username       VARCHAR(255) NOT NULL,
    display_name   VARCHAR(255) NOT NULL,
    email          VARCHAR(255),
    created_at     TIMESTAMP WITH TIME ZONE NOT NULL,
    last_login_at  TIMESTAMP WITH TIME ZONE
);

CREATE TABLE login_event (
    id           UUID PRIMARY KEY,
    user_id      UUID NOT NULL REFERENCES app_user (id),
    occurred_at  TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address   VARCHAR(64),
    user_agent   VARCHAR(512)
);

CREATE INDEX idx_login_event_user_time ON login_event (user_id, occurred_at DESC);
