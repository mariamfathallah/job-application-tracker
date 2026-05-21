--liquibase formatted sql

--changeset mariam:001-initial-schema
CREATE TABLE roles (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(50)  NOT NULL UNIQUE
);

CREATE TABLE users (
    id            BIGSERIAL    PRIMARY KEY,
    email         VARCHAR(190) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name  VARCHAR(120) NOT NULL
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id),
    role_id BIGINT NOT NULL REFERENCES roles(id),
    PRIMARY KEY (user_id, role_id)
);

CREATE TABLE job_applications (
    id           BIGSERIAL    PRIMARY KEY,
    company      VARCHAR(255) NOT NULL,
    position     VARCHAR(255) NOT NULL,
    status       VARCHAR(50)  NOT NULL,
    date_applied DATE         NOT NULL,
    notes        VARCHAR(2000),
    owner_id     BIGINT       NOT NULL REFERENCES users(id)
);
--rollback DROP TABLE job_applications;
--rollback DROP TABLE user_roles;
--rollback DROP TABLE users;
--rollback DROP TABLE roles;
