-- Seed product details (brand, MRP, warranty, seller, highlights, specifications, images)
-- for ALL products created by V1 (10 hand-written) and V2 (5,000 generated).
--
-- Fully deterministic (no RNG): every variation is derived from MOD(id, n) style arithmetic, so every
-- fresh environment gets identical data. Written SET-BASED (INSERT ... SELECT / MERGE from small VALUES
-- template tables joined to products) instead of one INSERT row per generated row, because Flyway runs
-- this on every start of a fresh DB and startup time matters.
--
-- Images are DUMMY placeholder URLs (placehold.co) - the owner replaces them later, either per product
-- via PUT /api/products/{id}/images or by editing the image section of this file for new environments.

-- =====================================================================================================
-- 1. Hand-written details for the 10 original (V1) products
-- =====================================================================================================

MERGE INTO products AS p
USING (VALUES
    ('APPLE-IP15P-128',  'Apple',   1099.99, '1 year limited warranty',          'Orbit Electronics Retail'),
    ('APPLE-MBP16-M3',   'Apple',   2799.99, '1 year limited warranty',          'Orbit Electronics Retail'),
    ('APPLE-APP2-WHT',   'Apple',    279.99, '1 year limited warranty',          'Orbit Electronics Retail'),
    ('SAMSUNG-S24-256',  'Samsung',  999.99, '1 year manufacturer warranty',     'Northwind Gadgets'),
    ('SONY-WH1000XM5',   'Sony',     449.99, '1 year manufacturer warranty',     'Northwind Gadgets'),
    ('DELL-XPS15-I9',    'Dell',    1999.99, '1 year limited hardware warranty', 'BrightCart Computers'),
    ('APPLE-IPAD-AIR',   'Apple',    649.99, '1 year limited warranty',          'Orbit Electronics Retail'),
    ('AMAZON-KDL-PW',    'Amazon',   159.99, '1 year limited warranty',          'Northwind Gadgets'),
    ('GOPRO-H12-BLK',    'GoPro',    499.99, '1 year limited warranty',          'BrightCart Computers'),
    ('DYSON-V15-DETECT', 'Dyson',    749.99, '2 year manufacturer warranty',     'HomeMart Retail')
) AS s(sku, brand, mrp, warranty, seller)
ON p.sku_code = s.sku
WHEN MATCHED THEN UPDATE SET brand = s.brand, mrp = s.mrp, warranty = s.warranty, seller = s.seller;

INSERT INTO product_highlights (product_id, text, sort_order)
SELECT p.id, h.hl, h.n
FROM products p
JOIN (VALUES
    ('APPLE-IP15P-128', 1, 'A17 Pro chip with a 6-core GPU for console-level gaming and fast everyday performance'),
    ('APPLE-IP15P-128', 2, '6.1-inch Super Retina XDR display with ProMotion up to 120 Hz'),
    ('APPLE-IP15P-128', 3, 'Pro camera system with a 48 MP Main camera and 3x optical zoom telephoto'),
    ('APPLE-IP15P-128', 4, 'Lightweight titanium design with a USB-C connector'),
    ('APPLE-IP15P-128', 5, 'Up to 23 hours of video playback'),

    ('APPLE-MBP16-M3', 1, 'M3 Max chip with up to a 16-core CPU and 40-core GPU for demanding pro workflows'),
    ('APPLE-MBP16-M3', 2, '16.2-inch Liquid Retina XDR display with up to 1600 nits peak HDR brightness'),
    ('APPLE-MBP16-M3', 3, 'Up to 22 hours of battery life'),
    ('APPLE-MBP16-M3', 4, 'Three Thunderbolt 4 ports, HDMI, SDXC card slot and MagSafe 3 charging'),
    ('APPLE-MBP16-M3', 5, '1080p FaceTime HD camera and six-speaker sound system with Spatial Audio'),

    ('APPLE-APP2-WHT', 1, 'H2 chip for smarter noise cancellation and richer sound'),
    ('APPLE-APP2-WHT', 2, 'Active Noise Cancellation with Adaptive Transparency'),
    ('APPLE-APP2-WHT', 3, 'Personalised Spatial Audio with dynamic head tracking'),
    ('APPLE-APP2-WHT', 4, 'Up to 6 hours of listening time, up to 30 hours with the charging case'),
    ('APPLE-APP2-WHT', 5, 'Dust, sweat and water resistant (IP54) earbuds and case'),

    ('SAMSUNG-S24-256', 1, '6.2-inch Dynamic AMOLED 2X display with a 120 Hz adaptive refresh rate'),
    ('SAMSUNG-S24-256', 2, '50 MP main camera with 3x optical zoom and Nightography low-light shooting'),
    ('SAMSUNG-S24-256', 3, 'Galaxy AI features including Live Translate and Circle to Search'),
    ('SAMSUNG-S24-256', 4, '256 GB storage with 8 GB RAM'),
    ('SAMSUNG-S24-256', 5, '7 years of OS and security updates'),

    ('SONY-WH1000XM5', 1, 'Industry-leading noise cancellation with two processors and eight microphones'),
    ('SONY-WH1000XM5', 2, 'Up to 30 hours of battery life with noise cancelling on'),
    ('SONY-WH1000XM5', 3, 'Quick charge: 3 minutes of charging gives up to 3 hours of playback'),
    ('SONY-WH1000XM5', 4, 'Crystal-clear hands-free calls with a beamforming microphone array'),
    ('SONY-WH1000XM5', 5, 'Lightweight, soft-fit design with multipoint Bluetooth connection'),

    ('DELL-XPS15-I9', 1, '15.6-inch 3.5K OLED InfinityEdge display with a 100 percent DCI-P3 colour gamut'),
    ('DELL-XPS15-I9', 2, 'Intel Core i9 processor with NVIDIA GeForce RTX graphics for creators'),
    ('DELL-XPS15-I9', 3, '32 GB DDR5 memory and a 1 TB NVMe solid-state drive'),
    ('DELL-XPS15-I9', 4, 'CNC machined aluminium chassis with a precision glass touchpad'),
    ('DELL-XPS15-I9', 5, 'Windows 11 Pro with Thunderbolt 4 connectivity'),

    ('APPLE-IPAD-AIR', 1, 'M1 chip for desktop-class performance in a thin and light design'),
    ('APPLE-IPAD-AIR', 2, '10.9-inch Liquid Retina display with True Tone and P3 wide colour'),
    ('APPLE-IPAD-AIR', 3, '12 MP Wide back camera and 12 MP Ultra Wide front camera with Center Stage'),
    ('APPLE-IPAD-AIR', 4, 'Works with Apple Pencil (2nd generation) and Magic Keyboard'),
    ('APPLE-IPAD-AIR', 5, 'USB-C port and Touch ID built into the top button'),

    ('AMAZON-KDL-PW', 1, '6.8-inch glare-free display with 300 ppi for paper-like reading'),
    ('AMAZON-KDL-PW', 2, 'Adjustable warm light for comfortable reading day and night'),
    ('AMAZON-KDL-PW', 3, 'Up to 10 weeks of battery life on a single charge'),
    ('AMAZON-KDL-PW', 4, 'Waterproof (IPX8) so you can read at the beach or by the pool'),
    ('AMAZON-KDL-PW', 5, '8 GB of storage holds thousands of books'),

    ('GOPRO-H12-BLK', 1, '5.3K60 and 4K120 video with HyperSmooth 6.0 stabilisation'),
    ('GOPRO-H12-BLK', 2, '27 MP photos and HDR video for rich, balanced colour'),
    ('GOPRO-H12-BLK', 3, 'Waterproof to 10 m without a housing'),
    ('GOPRO-H12-BLK', 4, 'Longer runtimes with the Enduro battery'),
    ('GOPRO-H12-BLK', 5, 'Bluetooth audio support for wireless microphones and headphones'),

    ('DYSON-V15-DETECT', 1, 'Laser illumination reveals microscopic dust on hard floors'),
    ('DYSON-V15-DETECT', 2, 'Piezo sensor counts and sizes particles and adapts suction automatically'),
    ('DYSON-V15-DETECT', 3, 'Up to 60 minutes of fade-free run time'),
    ('DYSON-V15-DETECT', 4, 'Whole-machine filtration captures 99.99 percent of particles down to 0.3 microns'),
    ('DYSON-V15-DETECT', 5, 'LCD screen shows real-time run time, performance and maintenance alerts')
) AS h(sku, n, hl) ON p.sku_code = h.sku;

INSERT INTO product_specifications (product_id, group_name, spec_key, spec_value, sort_order)
SELECT p.id, s.grp, s.skey, s.sval, s.n
FROM products p
JOIN (VALUES
    ('APPLE-IP15P-128', 1, 'Display',     'Screen Size',      '6.1 inches'),
    ('APPLE-IP15P-128', 2, 'Display',     'Resolution',       '2556 x 1179 pixels at 460 ppi'),
    ('APPLE-IP15P-128', 3, 'Display',     'Refresh Rate',     '120 Hz ProMotion'),
    ('APPLE-IP15P-128', 4, 'Performance', 'Chip',             'A17 Pro'),
    ('APPLE-IP15P-128', 5, 'Performance', 'Storage',          '128 GB'),
    ('APPLE-IP15P-128', 6, 'Performance', 'Battery',          'Up to 23 hours video playback'),
    ('APPLE-IP15P-128', 7, 'Camera',      'Main Camera',      '48 MP'),
    ('APPLE-IP15P-128', 8, 'Camera',      'Ultra Wide',       '12 MP'),
    ('APPLE-IP15P-128', 9, 'Camera',      'Telephoto',        '12 MP, 3x optical zoom'),

    ('APPLE-MBP16-M3', 1, 'Display',      'Screen Size',      '16.2 inches'),
    ('APPLE-MBP16-M3', 2, 'Display',      'Resolution',       '3456 x 2234 pixels'),
    ('APPLE-MBP16-M3', 3, 'Display',      'Brightness',       '1000 nits sustained, 1600 nits peak HDR'),
    ('APPLE-MBP16-M3', 4, 'Performance',  'Chip',             'Apple M3 Max'),
    ('APPLE-MBP16-M3', 5, 'Performance',  'Memory',           '36 GB unified memory'),
    ('APPLE-MBP16-M3', 6, 'Performance',  'Storage',          '1 TB SSD'),
    ('APPLE-MBP16-M3', 7, 'Connectivity', 'Ports',            '3x Thunderbolt 4, HDMI, SDXC, MagSafe 3'),
    ('APPLE-MBP16-M3', 8, 'Connectivity', 'Wireless',         'Wi-Fi 6E, Bluetooth 5.3'),
    ('APPLE-MBP16-M3', 9, 'Connectivity', 'Battery',          'Up to 22 hours'),

    ('APPLE-APP2-WHT', 1, 'Audio',        'Chip',             'Apple H2'),
    ('APPLE-APP2-WHT', 2, 'Audio',        'Noise Control',    'Active Noise Cancellation, Transparency, Adaptive Audio'),
    ('APPLE-APP2-WHT', 3, 'Audio',        'Sound',            'Personalised Spatial Audio with dynamic head tracking'),
    ('APPLE-APP2-WHT', 4, 'Battery',      'Listening Time',   'Up to 6 hours (earbuds)'),
    ('APPLE-APP2-WHT', 5, 'Battery',      'Total With Case',  'Up to 30 hours'),
    ('APPLE-APP2-WHT', 6, 'Battery',      'Charging',         'MagSafe, Qi, Apple Watch charger or USB-C'),
    ('APPLE-APP2-WHT', 7, 'General',      'Colour',           'White'),
    ('APPLE-APP2-WHT', 8, 'General',      'Water Resistance', 'IP54 (earbuds and case)'),

    ('SAMSUNG-S24-256', 1, 'Display',     'Screen Size',      '6.2 inches Dynamic AMOLED 2X'),
    ('SAMSUNG-S24-256', 2, 'Display',     'Resolution',       '2340 x 1080 pixels (FHD+)'),
    ('SAMSUNG-S24-256', 3, 'Display',     'Refresh Rate',     '1 - 120 Hz adaptive'),
    ('SAMSUNG-S24-256', 4, 'Performance', 'Processor',        'Octa-core (region dependent: Exynos 2400 or Snapdragon 8 Gen 3)'),
    ('SAMSUNG-S24-256', 5, 'Performance', 'RAM',              '8 GB'),
    ('SAMSUNG-S24-256', 6, 'Performance', 'Storage',          '256 GB'),
    ('SAMSUNG-S24-256', 7, 'Camera',      'Rear Camera',      '50 MP + 12 MP ultra wide + 10 MP telephoto'),
    ('SAMSUNG-S24-256', 8, 'Camera',      'Front Camera',     '12 MP'),
    ('SAMSUNG-S24-256', 9, 'Performance', 'Battery',          '4000 mAh with 25 W wired charging'),

    ('SONY-WH1000XM5', 1, 'Audio',        'Driver Unit',      '30 mm'),
    ('SONY-WH1000XM5', 2, 'Audio',        'Frequency Response', '4 Hz - 40,000 Hz'),
    ('SONY-WH1000XM5', 3, 'Audio',        'Noise Cancelling', 'Yes, with Integrated Processor V1 and HD Noise Cancelling Processor QN1'),
    ('SONY-WH1000XM5', 4, 'Connectivity', 'Bluetooth',        'Bluetooth 5.2 with multipoint'),
    ('SONY-WH1000XM5', 5, 'Connectivity', 'Codecs',           'SBC, AAC, LDAC'),
    ('SONY-WH1000XM5', 6, 'Battery',      'Battery Life',     'Up to 30 hours (noise cancelling on)'),
    ('SONY-WH1000XM5', 7, 'Battery',      'Charging',         'USB-C, 3 minutes gives up to 3 hours of playback'),
    ('SONY-WH1000XM5', 8, 'Battery',      'Weight',           'Approx. 250 g'),

    ('DELL-XPS15-I9', 1, 'Display',       'Screen Size',      '15.6 inches'),
    ('DELL-XPS15-I9', 2, 'Display',       'Resolution',       '3456 x 2160 pixels (3.5K OLED)'),
    ('DELL-XPS15-I9', 3, 'Performance',   'Processor',        'Intel Core i9 (13th Gen)'),
    ('DELL-XPS15-I9', 4, 'Performance',   'Graphics',         'NVIDIA GeForce RTX 4070 8 GB'),
    ('DELL-XPS15-I9', 5, 'Performance',   'Memory',           '32 GB DDR5'),
    ('DELL-XPS15-I9', 6, 'Performance',   'Storage',          '1 TB NVMe SSD'),
    ('DELL-XPS15-I9', 7, 'Connectivity',  'Ports',            '2x Thunderbolt 4, USB-C 3.2, SD card reader, headphone jack'),
    ('DELL-XPS15-I9', 8, 'Connectivity',  'Wireless',         'Wi-Fi 6E, Bluetooth 5.3'),
    ('DELL-XPS15-I9', 9, 'Connectivity',  'Weight',           'Approx. 1.92 kg'),

    ('APPLE-IPAD-AIR', 1, 'Display',      'Screen Size',      '10.9 inches Liquid Retina'),
    ('APPLE-IPAD-AIR', 2, 'Display',      'Resolution',       '2360 x 1640 pixels at 264 ppi'),
    ('APPLE-IPAD-AIR', 3, 'Display',      'Brightness',       '500 nits'),
    ('APPLE-IPAD-AIR', 4, 'Performance',  'Chip',             'Apple M1'),
    ('APPLE-IPAD-AIR', 5, 'Performance',  'Storage',          '64 GB'),
    ('APPLE-IPAD-AIR', 6, 'Performance',  'Battery',          'Up to 10 hours of web browsing or video'),
    ('APPLE-IPAD-AIR', 7, 'Connectivity', 'Wireless',         'Wi-Fi 6, Bluetooth 5.0'),
    ('APPLE-IPAD-AIR', 8, 'Connectivity', 'Port',             'USB-C'),
    ('APPLE-IPAD-AIR', 9, 'Connectivity', 'Cameras',          '12 MP Wide (rear), 12 MP Ultra Wide (front)'),

    ('AMAZON-KDL-PW', 1, 'Display',       'Screen Size',      '6.8 inches'),
    ('AMAZON-KDL-PW', 2, 'Display',       'Resolution',       '300 ppi, glare-free'),
    ('AMAZON-KDL-PW', 3, 'Display',       'Lighting',         'Adjustable warm light'),
    ('AMAZON-KDL-PW', 4, 'Storage and Battery', 'Storage',    '8 GB'),
    ('AMAZON-KDL-PW', 5, 'Storage and Battery', 'Battery Life', 'Up to 10 weeks'),
    ('AMAZON-KDL-PW', 6, 'Storage and Battery', 'Charging',   'USB-C'),
    ('AMAZON-KDL-PW', 7, 'General',       'Water Rating',     'IPX8 waterproof'),
    ('AMAZON-KDL-PW', 8, 'General',       'Connectivity',     'Wi-Fi'),
    ('AMAZON-KDL-PW', 9, 'General',       'Weight',           'Approx. 205 g'),

    ('GOPRO-H12-BLK', 1, 'Video',         'Max Resolution',   '5.3K at 60 fps'),
    ('GOPRO-H12-BLK', 2, 'Video',         'Slow Motion',      '4K at 120 fps, 2.7K at 240 fps'),
    ('GOPRO-H12-BLK', 3, 'Video',         'Stabilisation',    'HyperSmooth 6.0'),
    ('GOPRO-H12-BLK', 4, 'Photo',         'Resolution',       '27 MP'),
    ('GOPRO-H12-BLK', 5, 'Photo',         'HDR',              'HDR video and photo'),
    ('GOPRO-H12-BLK', 6, 'Build',         'Waterproof',       'Up to 10 m (33 ft)'),
    ('GOPRO-H12-BLK', 7, 'Build',         'Battery',          'Enduro, 1720 mAh'),
    ('GOPRO-H12-BLK', 8, 'Build',         'Connectivity',     'Wi-Fi 5, Bluetooth'),
    ('GOPRO-H12-BLK', 9, 'Build',         'Weight',           '154 g'),

    ('DYSON-V15-DETECT', 1, 'Performance', 'Suction Power',   '230 AW'),
    ('DYSON-V15-DETECT', 2, 'Performance', 'Run Time',        'Up to 60 minutes'),
    ('DYSON-V15-DETECT', 3, 'Performance', 'Bin Volume',      '0.76 L'),
    ('DYSON-V15-DETECT', 4, 'Filtration',  'Filter',          'Whole-machine HEPA filtration'),
    ('DYSON-V15-DETECT', 5, 'Filtration',  'Dust Detection',  'Laser illumination and piezo sensor'),
    ('DYSON-V15-DETECT', 6, 'Filtration',  'Display',         'LCD screen'),
    ('DYSON-V15-DETECT', 7, 'General',     'Charging Time',   'Approx. 4.5 hours'),
    ('DYSON-V15-DETECT', 8, 'General',     'Weight',          'Approx. 3.1 kg')
) AS s(sku, n, grp, skey, sval) ON p.sku_code = s.sku;

-- =====================================================================================================
-- 2. Generated products (5,000): SKU prefixes ELEC HOME CLTH BOOK SPRT TOYS GROC FURN BEAU AUTO
--    Templates below are matched to products by LEFT(sku_code, 4); the 10 hand-written SKUs above use
--    other prefixes and therefore never match a template row.
-- =====================================================================================================

-- 2a. brand (pool of 6 made-up names per category, picked by MOD(id, 6)), warranty, seller and
--     MRP = price x (1.10 .. 1.60, stepped by 0.01 via MOD) rounded to 2 decimals and never below price.
UPDATE products
SET brand = ARRAY_GET(
        CASE LEFT(sku_code, 4)
            WHEN 'ELEC' THEN ARRAY['Voltix', 'Nexora', 'Pulsar', 'Zentra', 'Lumio', 'Orbita']
            WHEN 'HOME' THEN ARRAY['HomeCrest', 'Aurelia', 'Kitchenova', 'Brightway', 'Pureflow', 'Nordhaus']
            WHEN 'CLTH' THEN ARRAY['Threadly', 'UrbanLoom', 'Cottonwood', 'Stitchline', 'Northfold', 'Weavewell']
            WHEN 'BOOK' THEN ARRAY['Maple Press', 'Lantern Books', 'Quillhouse', 'Pageturner', 'Inkwell Books', 'Horizon Publishing']
            WHEN 'SPRT' THEN ARRAY['TrailPeak', 'Summitra', 'Fieldmark', 'Aeroform', 'Ridgeline', 'Stridewell']
            WHEN 'TOYS' THEN ARRAY['Playnest', 'Funforge', 'Brickly', 'Toyvale', 'Wondermaker', 'Kidoodle']
            WHEN 'GROC' THEN ARRAY['Greenfield', 'Harvest Lane', 'Pantry Pure', 'Sunvale', 'Goodgrain', 'Farmhouse Fresh']
            WHEN 'FURN' THEN ARRAY['Oakridge', 'Nordic Nest', 'Casalume', 'Woodhaven', 'Loftwell', 'Homestead']
            WHEN 'BEAU' THEN ARRAY['Glowra', 'Pureleaf', 'Velvette', 'Dewdrop', 'Aurasilk', 'Botanica Labs']
            WHEN 'AUTO' THEN ARRAY['Torquemax', 'RoadReady', 'Autonova', 'Gearhead', 'Drivewell', 'Carbonix']
        END,
        MOD(id, 6) + 1),
    mrp = GREATEST(price, ROUND(price * (110 + MOD(id * 37 + 11, 51)) / 100, 2)),
    warranty = CASE LEFT(sku_code, 4)
        WHEN 'ELEC' THEN '1 year manufacturer warranty'
        WHEN 'HOME' THEN '2 year manufacturer warranty'
        WHEN 'CLTH' THEN '30 day replacement for manufacturing defects'
        WHEN 'BOOK' THEN 'No warranty'
        WHEN 'SPRT' THEN '6 month manufacturer warranty'
        WHEN 'TOYS' THEN '90 day limited warranty'
        WHEN 'GROC' THEN 'No warranty'
        WHEN 'FURN' THEN '1 year manufacturer warranty'
        WHEN 'BEAU' THEN 'No warranty'
        WHEN 'AUTO' THEN '1 year manufacturer warranty'
    END,
    seller = CASE LEFT(sku_code, 4)
        WHEN 'ELEC' THEN 'Orbit Electronics Retail'
        WHEN 'HOME' THEN 'HomeMart Retail'
        WHEN 'CLTH' THEN 'StyleStreet Fashion'
        WHEN 'BOOK' THEN 'Pageturner Booksellers'
        WHEN 'SPRT' THEN 'TrailHead Sports'
        WHEN 'TOYS' THEN 'PlayHouse Toys'
        WHEN 'GROC' THEN 'FreshBasket Grocers'
        WHEN 'FURN' THEN 'NestHome Furniture'
        WHEN 'BEAU' THEN 'GlowHub Beauty'
        WHEN 'AUTO' THEN 'RoadSide Auto Parts'
    END
WHERE LEFT(sku_code, 4) IN ('ELEC', 'HOME', 'CLTH', 'BOOK', 'SPRT', 'TOYS', 'GROC', 'FURN', 'BEAU', 'AUTO');

-- 2b. Per-product substitution values for the highlight/specification templates. Dropped at the end.
--     Tokens: {brand} {name} {sku} {type} {colour} {a} {b} {c} {d}; extra_rows (0..2) varies the row count.
CREATE VIEW seed_product_tokens AS
SELECT p.id,
       LEFT(p.sku_code, 4) AS prefix,
       p.sku_code,
       p.name,
       p.brand,
       LEFT(p.description, LOCATE(' - ', p.description) - 1) AS ptype,
       CASE MOD(p.id, 8)
           WHEN 0 THEN 'Black'  WHEN 1 THEN 'Silver'   WHEN 2 THEN 'White'    WHEN 3 THEN 'Graphite'
           WHEN 4 THEN 'Navy Blue' WHEN 5 THEN 'Midnight' WHEN 6 THEN 'Forest Green' ELSE 'Sand'
       END AS colour,
       CAST(10 + MOD(p.id * 7, 40) AS VARCHAR) AS a,
       CAST(100 + MOD(p.id * 13, 900) AS VARCHAR) AS b,
       CAST(1 + MOD(p.id, 9) AS VARCHAR) AS c,
       CAST(400 + MOD(p.id * 17, 1600) AS VARCHAR) AS d,
       MOD(p.id, 3) AS extra_rows
FROM products p
WHERE LEFT(p.sku_code, 4) IN ('ELEC', 'HOME', 'CLTH', 'BOOK', 'SPRT', 'TOYS', 'GROC', 'FURN', 'BEAU', 'AUTO')
  AND p.brand IS NOT NULL;

-- 2c. Highlights: 3 + extra_rows (3..5) of the 5 per-category templates.
INSERT INTO product_highlights (product_id, text, sort_order)
SELECT t.id,
       REPLACE(REPLACE(REPLACE(h.tpl, '{brand}', t.brand), '{type}', t.ptype), '{name}', t.name),
       h.n
FROM seed_product_tokens t
JOIN (VALUES
    ('ELEC', 1, '{brand} {type} designed for everyday performance'),
    ('ELEC', 2, 'Reliable connectivity with fast, stable pairing'),
    ('ELEC', 3, 'Energy-efficient design for long hours of use'),
    ('ELEC', 4, 'Compact, lightweight build that is easy to carry'),
    ('ELEC', 5, 'Backed by a 1 year manufacturer warranty'),

    ('HOME', 1, '{type} engineered by {brand} for everyday home use'),
    ('HOME', 2, 'Energy-efficient design keeps running costs low'),
    ('HOME', 3, 'Easy-clean parts that are simple to maintain'),
    ('HOME', 4, 'Built-in safety features for peace of mind'),
    ('HOME', 5, 'Backed by a 2 year manufacturer warranty'),

    ('CLTH', 1, '{brand} {type} crafted for all-day comfort'),
    ('CLTH', 2, 'Breathable, soft-touch material that keeps its shape'),
    ('CLTH', 3, 'Easy care that holds its colour wash after wash'),
    ('CLTH', 4, 'Versatile style that pairs with almost anything'),
    ('CLTH', 5, 'Available in a range of sizes'),

    ('BOOK', 1, '{type} published by {brand}'),
    ('BOOK', 2, 'A well-paced read with chapters that are easy to dip into'),
    ('BOOK', 3, 'Clear, easy-to-read print and layout'),
    ('BOOK', 4, 'A thoughtful gift for book lovers'),
    ('BOOK', 5, 'Durable binding built to last'),

    ('SPRT', 1, '{type} from {brand} built for active days'),
    ('SPRT', 2, 'Durable materials made for outdoor conditions'),
    ('SPRT', 3, 'Lightweight and easy to pack for trips'),
    ('SPRT', 4, 'Comfortable design for beginners and enthusiasts alike'),
    ('SPRT', 5, 'Backed by a 6 month manufacturer warranty'),

    ('TOYS', 1, '{type} from {brand} that sparks imagination'),
    ('TOYS', 2, 'Made with non-toxic, child-friendly materials'),
    ('TOYS', 3, 'Encourages creative, hands-on play'),
    ('TOYS', 4, 'Sturdy construction that stands up to rough play'),
    ('TOYS', 5, 'A great gift for birthdays and holidays'),

    ('GROC', 1, '{type} from {brand}, made with quality ingredients'),
    ('GROC', 2, 'No artificial colours or flavours added'),
    ('GROC', 3, 'Carefully packed to keep it fresh'),
    ('GROC', 4, 'Great for everyday cooking and snacking'),
    ('GROC', 5, 'Convenient pack size for the whole family'),

    ('FURN', 1, '{type} by {brand} that brings style to any room'),
    ('FURN', 2, 'Sturdy frame with a smooth, durable finish'),
    ('FURN', 3, 'Simple assembly with all fittings included'),
    ('FURN', 4, 'Space-smart design that suits modern homes'),
    ('FURN', 5, 'Backed by a 1 year manufacturer warranty'),

    ('BEAU', 1, '{type} from {brand} designed for daily care'),
    ('BEAU', 2, 'Gentle formula suitable for regular use'),
    ('BEAU', 3, 'Lightweight feel that absorbs quickly'),
    ('BEAU', 4, 'Made without artificial dyes'),
    ('BEAU', 5, 'Travel-friendly packaging'),

    ('AUTO', 1, '{type} by {brand} built for reliable road performance'),
    ('AUTO', 2, 'Durable construction that withstands heat and vibration'),
    ('AUTO', 3, 'Straightforward installation with clear instructions'),
    ('AUTO', 4, 'Fits a wide range of popular vehicles'),
    ('AUTO', 5, 'Backed by a 1 year manufacturer warranty')
) AS h(prefix, n, tpl)
  ON h.prefix = t.prefix AND h.n <= 3 + t.extra_rows;

-- 2d. Specifications: 6 + extra_rows (6..8) rows in two groups per category. Rows 1-3 and 7 belong to the
--     first group, rows 4-6 and 8 to the second; the API groups by group_name in order of first appearance.
--     vc/v pick one of vc value variants per product: the row with v = MOD(id + n, vc) is used (vc = 1: fixed text).
INSERT INTO product_specifications (product_id, group_name, spec_key, spec_value, sort_order)
SELECT t.id,
       s.grp,
       s.skey,
       REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(s.tpl,
           '{brand}', t.brand), '{name}', t.name), '{sku}', t.sku_code), '{type}', t.ptype), '{colour}', t.colour),
           '{a}', t.a), '{b}', t.b), '{c}', t.c), '{d}', t.d),
       s.n
FROM seed_product_tokens t
JOIN (VALUES
    -- Electronics
    ('ELEC', 1, 'General',   'Brand',        1, 0, '{brand}'),
    ('ELEC', 2, 'General',   'Model',        1, 0, '{type} {sku}'),
    ('ELEC', 3, 'General',   'Colour',       1, 0, '{colour}'),
    ('ELEC', 4, 'Technical', 'Battery',      1, 0, 'Up to {a} hours'),
    ('ELEC', 5, 'Technical', 'Connectivity', 4, 0, 'Bluetooth 5.3'),
    ('ELEC', 5, 'Technical', 'Connectivity', 4, 1, 'Wi-Fi 6 and Bluetooth 5.2'),
    ('ELEC', 5, 'Technical', 'Connectivity', 4, 2, 'USB-C and Bluetooth 5.0'),
    ('ELEC', 5, 'Technical', 'Connectivity', 4, 3, 'Wi-Fi 5 and USB 3.0'),
    ('ELEC', 6, 'Technical', 'Weight',       1, 0, '{b} g'),
    ('ELEC', 7, 'General',   'In the Box',   1, 0, '{type}, USB-C charging cable, quick start guide'),
    ('ELEC', 8, 'Technical', 'Power Input',  1, 0, '{a} W USB-C power delivery'),

    -- Home Appliances
    ('HOME', 1, 'General',   'Brand',        1, 0, '{brand}'),
    ('HOME', 2, 'General',   'Model',        1, 0, '{type} {sku}'),
    ('HOME', 3, 'General',   'Colour',       1, 0, '{colour}'),
    ('HOME', 4, 'Technical', 'Power',        1, 0, '{b} W'),
    ('HOME', 5, 'Technical', 'Voltage',      2, 0, '220-240 V, 50 Hz'),
    ('HOME', 5, 'Technical', 'Voltage',      2, 1, '110-120 V, 60 Hz'),
    ('HOME', 6, 'Technical', 'Energy Rating',3, 0, '3 Star'),
    ('HOME', 6, 'Technical', 'Energy Rating',3, 1, '4 Star'),
    ('HOME', 6, 'Technical', 'Energy Rating',3, 2, '5 Star'),
    ('HOME', 7, 'General',   'Included',     1, 0, '{type} unit, user manual, warranty card'),
    ('HOME', 8, 'Technical', 'Weight',       1, 0, '{c}.{a} kg'),

    -- Clothing
    ('CLTH', 1, 'General',     'Brand',      1, 0, '{brand}'),
    ('CLTH', 2, 'General',     'Fabric',     5, 0, '100 percent cotton'),
    ('CLTH', 2, 'General',     'Fabric',     5, 1, 'Linen blend'),
    ('CLTH', 2, 'General',     'Fabric',     5, 2, 'Polyester blend'),
    ('CLTH', 2, 'General',     'Fabric',     5, 3, 'Cotton with elastane'),
    ('CLTH', 2, 'General',     'Fabric',     5, 4, 'Wool blend'),
    ('CLTH', 3, 'General',     'Colour',     1, 0, '{colour}'),
    ('CLTH', 4, 'Fit and Care','Fit',        4, 0, 'Regular fit'),
    ('CLTH', 4, 'Fit and Care','Fit',        4, 1, 'Slim fit'),
    ('CLTH', 4, 'Fit and Care','Fit',        4, 2, 'Relaxed fit'),
    ('CLTH', 4, 'Fit and Care','Fit',        4, 3, 'Oversized fit'),
    ('CLTH', 5, 'Fit and Care','Care',       4, 0, 'Machine wash cold'),
    ('CLTH', 5, 'Fit and Care','Care',       4, 1, 'Machine wash warm, tumble dry low'),
    ('CLTH', 5, 'Fit and Care','Care',       4, 2, 'Hand wash only'),
    ('CLTH', 5, 'Fit and Care','Care',       4, 3, 'Dry clean only'),
    ('CLTH', 6, 'Fit and Care','Pattern',    4, 0, 'Solid'),
    ('CLTH', 6, 'Fit and Care','Pattern',    4, 1, 'Striped'),
    ('CLTH', 6, 'Fit and Care','Pattern',    4, 2, 'Checked'),
    ('CLTH', 6, 'Fit and Care','Pattern',    4, 3, 'Printed'),
    ('CLTH', 7, 'General',     'Sizes',      1, 0, 'Multiple sizes available - see the size chart'),
    ('CLTH', 8, 'Fit and Care','Country of Origin', 4, 0, 'India'),
    ('CLTH', 8, 'Fit and Care','Country of Origin', 4, 1, 'Bangladesh'),
    ('CLTH', 8, 'Fit and Care','Country of Origin', 4, 2, 'Vietnam'),
    ('CLTH', 8, 'Fit and Care','Country of Origin', 4, 3, 'Portugal'),

    -- Books
    ('BOOK', 1, 'General',      'Publisher', 1, 0, '{brand}'),
    ('BOOK', 2, 'General',      'Author',    8, 0, 'Avery Collins'),
    ('BOOK', 2, 'General',      'Author',    8, 1, 'Maya Thornton'),
    ('BOOK', 2, 'General',      'Author',    8, 2, 'Daniel Whitaker'),
    ('BOOK', 2, 'General',      'Author',    8, 3, 'Priya Nandakumar'),
    ('BOOK', 2, 'General',      'Author',    8, 4, 'Lucas Ferreira'),
    ('BOOK', 2, 'General',      'Author',    8, 5, 'Hannah Lindqvist'),
    ('BOOK', 2, 'General',      'Author',    8, 6, 'Omar Haddad'),
    ('BOOK', 2, 'General',      'Author',    8, 7, 'Sofia Marchetti'),
    ('BOOK', 3, 'General',      'Language',  5, 0, 'English'),
    ('BOOK', 3, 'General',      'Language',  5, 1, 'English'),
    ('BOOK', 3, 'General',      'Language',  5, 2, 'English'),
    ('BOOK', 3, 'General',      'Language',  5, 3, 'Spanish'),
    ('BOOK', 3, 'General',      'Language',  5, 4, 'French'),
    ('BOOK', 4, 'Book Details', 'Pages',     1, 0, '{b}'),
    ('BOOK', 5, 'Book Details', 'Format',    2, 0, 'Paperback'),
    ('BOOK', 5, 'Book Details', 'Format',    2, 1, 'Hardcover'),
    ('BOOK', 6, 'Book Details', 'Edition',   3, 0, '1st edition'),
    ('BOOK', 6, 'Book Details', 'Edition',   3, 1, '2nd edition'),
    ('BOOK', 6, 'Book Details', 'Edition',   3, 2, 'Revised edition'),
    ('BOOK', 7, 'General',      'Genre',     1, 0, '{type}'),
    ('BOOK', 8, 'Book Details', 'Weight',    1, 0, '{b} g'),

    -- Sports and Outdoors
    ('SPRT', 1, 'General',        'Brand',        1, 0, '{brand}'),
    ('SPRT', 2, 'General',        'Model',        1, 0, '{type} {sku}'),
    ('SPRT', 3, 'General',        'Colour',       1, 0, '{colour}'),
    ('SPRT', 4, 'Specifications', 'Material',     5, 0, 'Polyester'),
    ('SPRT', 4, 'Specifications', 'Material',     5, 1, 'Nylon'),
    ('SPRT', 4, 'Specifications', 'Material',     5, 2, 'Aluminium alloy'),
    ('SPRT', 4, 'Specifications', 'Material',     5, 3, 'EVA foam'),
    ('SPRT', 4, 'Specifications', 'Material',     5, 4, 'Natural rubber'),
    ('SPRT', 5, 'Specifications', 'Suitable For', 4, 0, 'Beginners'),
    ('SPRT', 5, 'Specifications', 'Suitable For', 4, 1, 'Intermediate users'),
    ('SPRT', 5, 'Specifications', 'Suitable For', 4, 2, 'Professionals'),
    ('SPRT', 5, 'Specifications', 'Suitable For', 4, 3, 'All skill levels'),
    ('SPRT', 6, 'Specifications', 'Weight',       1, 0, '{b} g'),
    ('SPRT', 7, 'General',        'Season',       3, 0, 'All seasons'),
    ('SPRT', 7, 'General',        'Season',       3, 1, 'Spring and summer'),
    ('SPRT', 7, 'General',        'Season',       3, 2, 'Autumn and winter'),
    ('SPRT', 8, 'Specifications', 'Packed Size',  1, 0, '{d} x {b} mm'),

    -- Toys and Games
    ('TOYS', 1, 'General',      'Brand',       1, 0, '{brand}'),
    ('TOYS', 2, 'General',      'Age Group',   4, 0, '3+ years'),
    ('TOYS', 2, 'General',      'Age Group',   4, 1, '5+ years'),
    ('TOYS', 2, 'General',      'Age Group',   4, 2, '8+ years'),
    ('TOYS', 2, 'General',      'Age Group',   4, 3, '12+ years'),
    ('TOYS', 3, 'General',      'Colour',      1, 0, '{colour}'),
    ('TOYS', 4, 'Play Details', 'Material',    4, 0, 'ABS plastic'),
    ('TOYS', 4, 'Play Details', 'Material',    4, 1, 'Wood'),
    ('TOYS', 4, 'Play Details', 'Material',    4, 2, 'Soft fabric'),
    ('TOYS', 4, 'Play Details', 'Material',    4, 3, 'Die-cast metal'),
    ('TOYS', 5, 'Play Details', 'Batteries',   3, 0, 'Not required'),
    ('TOYS', 5, 'Play Details', 'Batteries',   3, 1, '2 x AA batteries (not included)'),
    ('TOYS', 5, 'Play Details', 'Batteries',   3, 2, 'Rechargeable battery included'),
    ('TOYS', 6, 'Play Details', 'Players',     3, 0, '1 player'),
    ('TOYS', 6, 'Play Details', 'Players',     3, 1, '2-4 players'),
    ('TOYS', 6, 'Play Details', 'Players',     3, 2, '1-6 players'),
    ('TOYS', 7, 'General',      'Safety',      1, 0, 'Non-toxic, BPA-free materials'),
    ('TOYS', 8, 'Play Details', 'Package Weight', 1, 0, '{b} g'),

    -- Groceries
    ('GROC', 1, 'General',         'Brand',       1, 0, '{brand}'),
    ('GROC', 2, 'General',         'Pack Size',   6, 0, '250 g'),
    ('GROC', 2, 'General',         'Pack Size',   6, 1, '500 g'),
    ('GROC', 2, 'General',         'Pack Size',   6, 2, '1 kg'),
    ('GROC', 2, 'General',         'Pack Size',   6, 3, '2 kg'),
    ('GROC', 2, 'General',         'Pack Size',   6, 4, '750 ml'),
    ('GROC', 2, 'General',         'Pack Size',   6, 5, '1 L'),
    ('GROC', 3, 'General',         'Dietary Type',4, 0, 'Vegetarian'),
    ('GROC', 3, 'General',         'Dietary Type',4, 1, 'Vegan'),
    ('GROC', 3, 'General',         'Dietary Type',4, 2, 'Gluten free'),
    ('GROC', 3, 'General',         'Dietary Type',4, 3, 'Organic'),
    ('GROC', 4, 'Product Details', 'Shelf Life',  4, 0, '6 months'),
    ('GROC', 4, 'Product Details', 'Shelf Life',  4, 1, '9 months'),
    ('GROC', 4, 'Product Details', 'Shelf Life',  4, 2, '12 months'),
    ('GROC', 4, 'Product Details', 'Shelf Life',  4, 3, '18 months'),
    ('GROC', 5, 'Product Details', 'Storage',     3, 0, 'Store in a cool, dry place'),
    ('GROC', 5, 'Product Details', 'Storage',     3, 1, 'Refrigerate after opening'),
    ('GROC', 5, 'Product Details', 'Storage',     3, 2, 'Keep away from direct sunlight'),
    ('GROC', 6, 'Product Details', 'Country of Origin', 4, 0, 'India'),
    ('GROC', 6, 'Product Details', 'Country of Origin', 4, 1, 'Italy'),
    ('GROC', 6, 'Product Details', 'Country of Origin', 4, 2, 'Spain'),
    ('GROC', 6, 'Product Details', 'Country of Origin', 4, 3, 'New Zealand'),
    ('GROC', 7, 'General',         'Ingredients', 1, 0, '{type} and natural ingredients - see the pack for the full list'),
    ('GROC', 8, 'Product Details', 'Allergen Information', 1, 0, 'Please check the pack for allergen details'),

    -- Furniture
    ('FURN', 1, 'General',                'Brand',    1, 0, '{brand}'),
    ('FURN', 2, 'General',                'Model',    1, 0, '{type} {sku}'),
    ('FURN', 3, 'General',                'Finish',   5, 0, 'Walnut'),
    ('FURN', 3, 'General',                'Finish',   5, 1, 'Natural oak'),
    ('FURN', 3, 'General',                'Finish',   5, 2, 'Espresso'),
    ('FURN', 3, 'General',                'Finish',   5, 3, 'White'),
    ('FURN', 3, 'General',                'Finish',   5, 4, 'Charcoal'),
    ('FURN', 4, 'Dimensions and Material','Material', 4, 0, 'Engineered wood'),
    ('FURN', 4, 'Dimensions and Material','Material', 4, 1, 'Solid pine'),
    ('FURN', 4, 'Dimensions and Material','Material', 4, 2, 'Metal frame with wooden top'),
    ('FURN', 4, 'Dimensions and Material','Material', 4, 3, 'Rubberwood'),
    ('FURN', 5, 'Dimensions and Material','Width',    1, 0, '{d} mm'),
    ('FURN', 6, 'Dimensions and Material','Depth',    1, 0, '{b} mm'),
    ('FURN', 7, 'General',                'Assembly', 2, 0, 'Self-assembly, tools included'),
    ('FURN', 7, 'General',                'Assembly', 2, 1, 'Partially assembled'),
    ('FURN', 8, 'Dimensions and Material','Weight Capacity', 1, 0, '{a}0 kg'),

    -- Beauty and Personal Care
    ('BEAU', 1, 'General',         'Brand',        1, 0, '{brand}'),
    ('BEAU', 2, 'General',         'Suitable For', 4, 0, 'All skin and hair types'),
    ('BEAU', 2, 'General',         'Suitable For', 4, 1, 'Dry skin and hair'),
    ('BEAU', 2, 'General',         'Suitable For', 4, 2, 'Oily skin and hair'),
    ('BEAU', 2, 'General',         'Suitable For', 4, 3, 'Sensitive skin'),
    ('BEAU', 3, 'General',         'Fragrance',    4, 0, 'Unscented'),
    ('BEAU', 3, 'General',         'Fragrance',    4, 1, 'Rose'),
    ('BEAU', 3, 'General',         'Fragrance',    4, 2, 'Lavender'),
    ('BEAU', 3, 'General',         'Fragrance',    4, 3, 'Citrus'),
    ('BEAU', 4, 'Product Details', 'Net Quantity', 4, 0, '30 ml'),
    ('BEAU', 4, 'Product Details', 'Net Quantity', 4, 1, '50 ml'),
    ('BEAU', 4, 'Product Details', 'Net Quantity', 4, 2, '100 ml'),
    ('BEAU', 4, 'Product Details', 'Net Quantity', 4, 3, '200 ml'),
    ('BEAU', 5, 'Product Details', 'Shelf Life',   1, 0, '24 months'),
    ('BEAU', 6, 'Product Details', 'Key Ingredient', 4, 0, 'Hyaluronic acid'),
    ('BEAU', 6, 'Product Details', 'Key Ingredient', 4, 1, 'Vitamin C'),
    ('BEAU', 6, 'Product Details', 'Key Ingredient', 4, 2, 'Aloe vera'),
    ('BEAU', 6, 'Product Details', 'Key Ingredient', 4, 3, 'Shea butter'),
    ('BEAU', 7, 'General',         'Usage',        1, 0, 'Use as directed on the pack'),
    ('BEAU', 8, 'Product Details', 'Country of Origin', 3, 0, 'India'),
    ('BEAU', 8, 'Product Details', 'Country of Origin', 3, 1, 'France'),
    ('BEAU', 8, 'Product Details', 'Country of Origin', 3, 2, 'South Korea'),

    -- Automotive
    ('AUTO', 1, 'General',   'Brand',        1, 0, '{brand}'),
    ('AUTO', 2, 'General',   'Model',        1, 0, '{type} {sku}'),
    ('AUTO', 3, 'General',   'Colour',       1, 0, '{colour}'),
    ('AUTO', 4, 'Technical', 'Material',     4, 0, 'ABS plastic'),
    ('AUTO', 4, 'Technical', 'Material',     4, 1, 'Stainless steel'),
    ('AUTO', 4, 'Technical', 'Material',     4, 2, 'Rubber'),
    ('AUTO', 4, 'Technical', 'Material',     4, 3, 'Aluminium alloy'),
    ('AUTO', 5, 'Technical', 'Power Supply', 3, 0, '12 V DC'),
    ('AUTO', 5, 'Technical', 'Power Supply', 3, 1, 'Not required'),
    ('AUTO', 5, 'Technical', 'Power Supply', 3, 2, '12 V DC (cigarette lighter socket)'),
    ('AUTO', 6, 'Technical', 'Weight',       1, 0, '{b} g'),
    ('AUTO', 7, 'General',   'Fitment',      3, 0, 'Universal fit'),
    ('AUTO', 7, 'General',   'Fitment',      3, 1, 'Most sedans and hatchbacks'),
    ('AUTO', 7, 'General',   'Fitment',      3, 2, 'SUVs and crossovers'),
    ('AUTO', 8, 'Technical', 'Installation', 2, 0, 'Easy self-installation'),
    ('AUTO', 8, 'Technical', 'Installation', 2, 1, 'Professional installation recommended')
) AS s(prefix, n, grp, skey, vc, v, tpl)
  ON s.prefix = t.prefix AND s.n <= 6 + t.extra_rows AND s.v = MOD(t.id + s.n, s.vc);

DROP VIEW seed_product_tokens;

-- =====================================================================================================
-- 3. Images for ALL products (originals + generated): 4 DUMMY placeholder images each.
--    https://placehold.co/800x800/<bg>/E6EDF7/png?text=<SKU>+<n>  (bg is one of four dark colours, by n).
--    alt = product name + ' - view n'. products.image_url then mirrors the sort_order = 1 image.
-- =====================================================================================================

INSERT INTO product_images (product_id, url, alt, sort_order)
SELECT p.id,
       'https://placehold.co/800x800/' || v.bg || '/E6EDF7/png?text=' || p.sku_code || '+' || CAST(v.n AS VARCHAR),
       p.name || ' - view ' || CAST(v.n AS VARCHAR),
       v.n
FROM products p
CROSS JOIN (VALUES
    (1, '172740'),
    (2, '1D3050'),
    (3, '2A3F5F'),
    (4, '0F1B2D')
) AS v(n, bg);

UPDATE products
SET image_url = (SELECT i.url FROM product_images i WHERE i.product_id = products.id AND i.sort_order = 1);
