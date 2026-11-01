-- ============================================================================
-- Smart Inventory & Expiry Management System
-- Database Schema Definition (MySQL 8.x)
-- ============================================================================

CREATE DATABASE IF NOT EXISTS smart_inventory 
    CHARACTER SET utf8mb4 
    COLLATE utf8mb4_unicode_ci;

USE smart_inventory;

-- ----------------------------------------------------------------------------
-- 1. Table: users
-- Stores application operators with roles and hashed credentials
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) UNIQUE,
    role ENUM('ADMIN', 'MANAGER', 'CASHIER') NOT NULL DEFAULT 'CASHIER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_username (username),
    INDEX idx_user_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 2. Table: categories
-- Classifies products into distinct retail groups/departments
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS categories (
    category_id INT AUTO_INCREMENT PRIMARY KEY,
    category_name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_category_name (category_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 3. Table: suppliers
-- Maintains vendor directory and procurement contacts
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id INT AUTO_INCREMENT PRIMARY KEY,
    supplier_name VARCHAR(150) NOT NULL,
    contact_person VARCHAR(100),
    phone VARCHAR(25) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_supplier_name (supplier_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 4. Table: products
-- Master catalog items (attributes independent of individual batches)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS products (
    product_id INT AUTO_INCREMENT PRIMARY KEY,
    category_id INT NOT NULL,
    sku VARCHAR(50) NOT NULL UNIQUE,
    barcode VARCHAR(100) UNIQUE,
    product_name VARCHAR(150) NOT NULL,
    description TEXT,
    unit VARCHAR(20) NOT NULL DEFAULT 'pcs',
    min_stock_level INT NOT NULL DEFAULT 10,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_products_category
        FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_product_sku (sku),
    INDEX idx_product_barcode (barcode),
    INDEX idx_product_name (product_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 5. Table: product_batches
-- Physical inventory lots tracking quantity, pricing, and expiration dates
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS product_batches (
    batch_id INT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    supplier_id INT NOT NULL,
    batch_number VARCHAR(50) NOT NULL,
    purchase_price DECIMAL(10, 2) NOT NULL CHECK (purchase_price >= 0),
    selling_price DECIMAL(10, 2) NOT NULL CHECK (selling_price >= 0),
    initial_quantity INT NOT NULL CHECK (initial_quantity >= 0),
    current_quantity INT NOT NULL CHECK (current_quantity >= 0),
    manufacturing_date DATE,
    expiry_date DATE NOT NULL,
    status ENUM('ACTIVE', 'EXPIRING_SOON', 'EXPIRED', 'DEPLETED') NOT NULL DEFAULT 'ACTIVE',
    received_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_batch_prices CHECK (selling_price >= purchase_price),
    CONSTRAINT fk_batches_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_batches_supplier
        FOREIGN KEY (supplier_id) REFERENCES suppliers(supplier_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT uq_product_batch
        UNIQUE (product_id, batch_number),
    INDEX idx_batch_expiry (expiry_date),
    INDEX idx_batch_status (status),
    INDEX idx_batch_quantity (current_quantity)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- 6. Table: stock_transactions
-- Immutable ledger recording every stock movement (IN/OUT/ADJUST/EXPIRED)
-- ----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS stock_transactions (
    transaction_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id INT NOT NULL,
    batch_id INT NOT NULL,
    user_id INT NOT NULL,
    transaction_type ENUM('PURCHASE_IN', 'SALE_OUT', 'RETURN_IN', 'DAMAGE_OUT', 'EXPIRED_WRITE_OFF') NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    balance_after INT NOT NULL CHECK (balance_after >= 0),
    reference_note VARCHAR(255),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_trans_product
        FOREIGN KEY (product_id) REFERENCES products(product_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_trans_batch
        FOREIGN KEY (batch_id) REFERENCES product_batches(batch_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    CONSTRAINT fk_trans_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON UPDATE CASCADE ON DELETE RESTRICT,
    INDEX idx_trans_product (product_id),
    INDEX idx_trans_batch (batch_id),
    INDEX idx_trans_date (created_at),
    INDEX idx_trans_type (transaction_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ----------------------------------------------------------------------------
-- Initial Default Seed Data
-- ----------------------------------------------------------------------------

-- Default Administrator account (password: admin123, BCrypt hash: $2a$12$e8r0.5d9yK9kUjRk5P8R1.k98sQxK6QzX/qZ5cO7f2.1A3W0j0B6a)
INSERT IGNORE INTO users (user_id, username, password_hash, full_name, email, role, is_active)
VALUES (1, 'admin', '$2a$12$e8r0.5d9yK9kUjRk5P8R1.k98sQxK6QzX/qZ5cO7f2.1A3W0j0B6a', 'System Administrator', 'admin@smartinventory.local', 'ADMIN', TRUE);

-- Initial Categories
INSERT IGNORE INTO categories (category_id, category_name, description) VALUES
(1, 'Beverages', 'Soft drinks, juices, energy drinks, and bottled water'),
(2, 'Dairy & Eggs', 'Milk, yogurt, cheese, butter, and farm eggs'),
(3, 'Bakery', 'Fresh bread, buns, pastries, and biscuits'),
(4, 'Pharmaceuticals', 'Over-the-counter medicines, supplements, and first-aid'),
(5, 'Packaged Foods', 'Cereals, canned foods, pasta, and instant noodles');
