--liquibase formatted sql

--changeset mariam:002-add-timestamps
ALTER TABLE job_applications ADD COLUMN created_at TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE job_applications ADD COLUMN updated_at TIMESTAMP NOT NULL DEFAULT NOW();
--rollback ALTER TABLE job_applications DROP COLUMN updated_at;
--rollback ALTER TABLE job_applications DROP COLUMN created_at;