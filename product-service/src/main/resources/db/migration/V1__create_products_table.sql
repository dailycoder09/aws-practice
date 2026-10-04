-- Create products table with proper constraints and indexes
CREATE TABLE IF NOT EXISTS products (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    price DECIMAL(10, 2) NOT NULL,
    category VARCHAR(100),
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_price CHECK (price >= 0),
    CONSTRAINT chk_name_not_empty CHECK (LENGTH(TRIM(name)) > 0),
    CONSTRAINT chk_sku_not_empty CHECK (LENGTH(TRIM(sku_code)) > 0)
);

-- Create indexes for frequently queried columns
CREATE INDEX idx_products_sku_code ON products(sku_code);
CREATE INDEX idx_products_category ON products(category);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_created_at ON products(created_at DESC);

-- Note: updated_at is maintained by Hibernate's @UpdateTimestamp on the
-- Product entity, not a DB-level trigger (H2 has no PL/pgSQL equivalent).

-- Insert sample data for testing
INSERT INTO products (name, description, price, category, sku_code) VALUES
    ('iPhone 15 Pro', 'Latest Apple flagship smartphone with A17 Pro chip', 999.99, 'Electronics', 'APPLE-IP15P-128'),
    ('MacBook Pro 16"', 'Professional laptop with M3 Max chip', 2499.99, 'Electronics', 'APPLE-MBP16-M3'),
    ('AirPods Pro 2', 'Wireless earbuds with active noise cancellation', 249.99, 'Electronics', 'APPLE-APP2-WHT'),
    ('Samsung Galaxy S24', 'Android flagship with advanced camera system', 899.99, 'Electronics', 'SAMSUNG-S24-256'),
    ('Sony WH-1000XM5', 'Premium wireless noise-canceling headphones', 399.99, 'Electronics', 'SONY-WH1000XM5'),
    ('Dell XPS 15', 'High-performance laptop for professionals', 1799.99, 'Electronics', 'DELL-XPS15-I9'),
    ('iPad Air', 'Versatile tablet with M1 chip', 599.99, 'Electronics', 'APPLE-IPAD-AIR'),
    ('Kindle Paperwhite', 'E-reader with adjustable warm light', 139.99, 'Electronics', 'AMAZON-KDL-PW'),
    ('GoPro HERO 12', 'Action camera with 5.3K video', 449.99, 'Electronics', 'GOPRO-H12-BLK'),
    ('Dyson V15', 'Cordless vacuum with laser detection', 649.99, 'Home Appliances', 'DYSON-V15-DETECT');
