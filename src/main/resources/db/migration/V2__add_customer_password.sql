--liquibase formatted sql

--changeset migration:V2-add-customer-password

ALTER TABLE customers
    ADD COLUMN password_hash VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE customers
    ALTER COLUMN password_hash DROP DEFAULT;
