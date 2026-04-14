--liquibase formatted sql

--changeset migration:V1-create-order-schema

CREATE TABLE customers
(
    id         UUID         NOT NULL,
    email      VARCHAR(255) NOT NULL,
    full_name  VARCHAR(255) NOT NULL,
    phone      VARCHAR(50),
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_customers PRIMARY KEY (id),
    CONSTRAINT uq_customers_email UNIQUE (email)
);

CREATE TABLE orders
(
    id           UUID          NOT NULL,
    order_number VARCHAR(20)   NOT NULL,
    status       VARCHAR(20)   NOT NULL,
    customer_id  UUID          NOT NULL,
    total_amount DECIMAL(12,2),
    street       VARCHAR(255),
    city         VARCHAR(100),
    state        VARCHAR(100),
    zip_code     VARCHAR(20),
    country      VARCHAR(100),
    created_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP     NOT NULL DEFAULT NOW(),
    version      BIGINT,
    CONSTRAINT pk_orders             PRIMARY KEY (id),
    CONSTRAINT uq_orders_number      UNIQUE (order_number),
    CONSTRAINT fk_orders_customer    FOREIGN KEY (customer_id) REFERENCES customers (id)
);

CREATE INDEX idx_orders_order_number ON orders (order_number);
CREATE INDEX idx_orders_customer_id  ON orders (customer_id);

CREATE TABLE order_items
(
    id           UUID          NOT NULL,
    order_id     UUID          NOT NULL,
    product_id   VARCHAR(255)  NOT NULL,
    product_name VARCHAR(255)  NOT NULL,
    quantity     INT           NOT NULL,
    unit_price   DECIMAL(10,2) NOT NULL,
    subtotal     DECIMAL(10,2) NOT NULL,
    CONSTRAINT pk_order_items          PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order    FOREIGN KEY (order_id) REFERENCES orders (id),
    CONSTRAINT chk_order_items_qty_pos CHECK (quantity > 0)
);
