CREATE TABLE refresh_tokens
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    user_id    bigint            NOT NULL,
    token_hash varchar(255)      NOT NULL,
    expires_at datetimeoffset(6) NOT NULL,
    revoked    bit               NOT NULL DEFAULT 0,
    created_at datetimeoffset(6) NOT NULL,
    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
);

ALTER TABLE refresh_tokens
    ADD CONSTRAINT uk_refresh_tokens_token_hash UNIQUE (token_hash);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
