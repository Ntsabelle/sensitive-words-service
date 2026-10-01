CREATE TABLE sensitive_words
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    word       varchar(255) NOT NULL,
    active     bit          NOT NULL,
    created_at datetimeoffset(6) NOT NULL,
    updated_at datetimeoffset(6) NOT NULL,
    CONSTRAINT pk_sensitive_words PRIMARY KEY (id)
);

CREATE TABLE users
(
    id         bigint IDENTITY (1, 1) NOT NULL,
    username   varchar(255) NOT NULL,
    password   varchar(255) NOT NULL,
    active     bit          NOT NULL,
    created_at datetimeoffset(6) NOT NULL,
    updated_at datetimeoffset(6),
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_username UNIQUE (username);

ALTER TABLE sensitive_words
    ADD CONSTRAINT uk_sensitive_words_word UNIQUE (word);