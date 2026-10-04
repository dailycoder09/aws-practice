-- Create inventory_items table with proper constraints and indexes
CREATE TABLE IF NOT EXISTS inventory_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(50) UNIQUE NOT NULL,
    quantity_on_hand INT NOT NULL,
    reorder_threshold INT NOT NULL DEFAULT 10,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_quantity_on_hand CHECK (quantity_on_hand >= 0),
    CONSTRAINT chk_inventory_sku_not_empty CHECK (LENGTH(TRIM(sku_code)) > 0)
);

-- Create index for frequently queried column
CREATE INDEX idx_inventory_items_sku_code ON inventory_items(sku_code);

-- Note: updated_at is maintained by Hibernate's @UpdateTimestamp on the
-- InventoryItem entity, not a DB-level trigger (H2 has no PL/pgSQL equivalent).
-- sku_code is NOT a database-level foreign key to product-service's products
-- table - the two services own separate databases. Validity is enforced via
-- the REST call to product-service at write time (see ProductClient).

-- Create inventory_audit_log table: an append-only record of every stock
-- change (create/adjust/update/delete), so the audit trail can never drift
-- from the inventory_items table it describes (both are written in the same
-- transaction boundary in the service layer).
CREATE TABLE IF NOT EXISTS inventory_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sku_code VARCHAR(50) NOT NULL,
    change_type VARCHAR(20) NOT NULL,
    previous_quantity INT,
    new_quantity INT,
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_audit_sku_not_empty CHECK (LENGTH(TRIM(sku_code)) > 0)
);

-- Create index for frequently queried column (audit history lookups are
-- always scoped to a single SKU, most-recent-first)
CREATE INDEX idx_inventory_audit_log_sku_code ON inventory_audit_log(sku_code);
