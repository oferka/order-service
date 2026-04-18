--liquibase formatted sql

--changeset migration:V3-add-customer-role

ALTER TABLE customers
    ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'ROLE_USER';
