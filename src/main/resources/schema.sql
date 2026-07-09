-- Schema for the demand-supply matching application (Part 2 onward).
-- Run this once against your MySQL server before starting the app:
--   mysql -u root -p < src/main/resources/schema.sql

CREATE DATABASE IF NOT EXISTS demand_supply_db;
USE demand_supply_db;

CREATE TABLE IF NOT EXISTS orders (
    id                 VARCHAR(50)  NOT NULL PRIMARY KEY,
    order_type         VARCHAR(10)  NOT NULL,               -- SUPPLY or DEMAND
    produce            VARCHAR(100) NOT NULL,
    price              DECIMAL(10,2) NOT NULL,
    original_quantity  INT          NOT NULL,
    remaining_quantity INT          NOT NULL,
    order_time         DATETIME     NOT NULL,
    sequence_no        BIGINT       NOT NULL,                -- ingestion order, used for FIFO tie-breaks
    status             VARCHAR(20)  NOT NULL,                -- PENDING, PARTIALLY_FILLED, FILLED
    created_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_produce_type (produce, order_type),
    INDEX idx_sequence (sequence_no)
);

CREATE TABLE IF NOT EXISTS trades (
    id               BIGINT AUTO_INCREMENT PRIMARY KEY,
    demand_order_id  VARCHAR(50) NOT NULL,
    supply_order_id  VARCHAR(50) NOT NULL,
    price            DECIMAL(10,2) NOT NULL,
    quantity         INT NOT NULL,
    trade_time       DATETIME NOT NULL,
    FOREIGN KEY (demand_order_id) REFERENCES orders(id),
    FOREIGN KEY (supply_order_id) REFERENCES orders(id)
);
