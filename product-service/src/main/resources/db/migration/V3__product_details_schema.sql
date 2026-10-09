-- Product details: brand / MRP / warranty / seller on the product row, plus child tables for
-- images, highlights and specifications (the "product detail page" data).
-- Never edit V1/V2; schema evolves through new migrations only.

-- Scalar detail columns (all optional). image_url is the denormalised primary image
-- (the sort_order = 1 row of product_images) so list/search queries never need the images table.
ALTER TABLE products ADD COLUMN brand VARCHAR(100);
ALTER TABLE products ADD COLUMN mrp DECIMAL(10, 2);
ALTER TABLE products ADD COLUMN warranty VARCHAR(100);
ALTER TABLE products ADD COLUMN seller VARCHAR(100);
ALTER TABLE products ADD COLUMN image_url VARCHAR(500);

ALTER TABLE products ADD CONSTRAINT chk_mrp CHECK (mrp IS NULL OR mrp >= 0);

CREATE INDEX idx_products_brand ON products(brand);

-- Product images: URLs only (no upload / file storage). Position is sort_order (1-based, unique per product).
CREATE TABLE product_images (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    url VARCHAR(500) NOT NULL,
    alt VARCHAR(255),
    sort_order INT NOT NULL,

    CONSTRAINT fk_product_images_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT uq_product_images_order UNIQUE (product_id, sort_order)
);

CREATE INDEX idx_product_images_product_id ON product_images(product_id);

-- Product highlights: short bullet points shown at the top of the detail page.
CREATE TABLE product_highlights (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    text VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,

    CONSTRAINT fk_product_highlights_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_highlights_product_id ON product_highlights(product_id);

-- Product specifications: key/value rows, grouped by group_name on read.
CREATE TABLE product_specifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    product_id BIGINT NOT NULL,
    group_name VARCHAR(100) NOT NULL,
    spec_key VARCHAR(100) NOT NULL,
    spec_value VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,

    CONSTRAINT fk_product_specifications_product FOREIGN KEY (product_id) REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_specifications_product_id ON product_specifications(product_id);
