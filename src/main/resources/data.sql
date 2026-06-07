-- ============================================================
-- EcoRoute Multi-Tenant Seed Data
-- Runs after Hibernate creates tables (ddl-auto=create)
-- ============================================================

-- ── Companies (Tenants) ──────────────────────────────────────
INSERT IGNORE INTO companies (name, industry_sector, onboarding_status, created_at) VALUES
    ('GreenFleet Logistics',      'E-Commerce & Retail',   'ACTIVE', NOW()),
    ('OceanCargo International',  'Maritime Shipping',     'ACTIVE', NOW()),
    ('EcoRoute System',           'Platform Administration', 'ACTIVE', NOW());

-- ── Users (BCrypt hash of 'password') ───────────────────────
-- System Admin user
INSERT IGNORE INTO users (username, password, role, company_id) VALUES
    ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'SYSTEM_ADMIN',
        (SELECT id FROM companies WHERE name = 'EcoRoute System'));

-- GreenFleet Logistics users
INSERT IGNORE INTO users (username, password, role, company_id) VALUES
    ('manager1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LOGISTICS_MANAGER',
        (SELECT id FROM companies WHERE name = 'GreenFleet Logistics')),
    ('auditor1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUDITOR',
        (SELECT id FROM companies WHERE name = 'GreenFleet Logistics'));

-- OceanCargo International users
INSERT IGNORE INTO users (username, password, role, company_id) VALUES
    ('manager2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LOGISTICS_MANAGER',
        (SELECT id FROM companies WHERE name = 'OceanCargo International')),
    ('auditor2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'AUDITOR',
        (SELECT id FROM companies WHERE name = 'OceanCargo International'));

-- ── Distance Lookup Matrix (JdbcTemplate queries) ────────────
INSERT IGNORE INTO distance_lookups (city_a, city_b, distance_km) VALUES
    ('Bengaluru', 'Mysuru',   145.0),
    ('Bengaluru', 'Mumbai',   980.5),
    ('Chennai',   'Bengaluru', 350.2),
    ('Mumbai',    'Hyderabad', 711.5),
    ('Hyderabad', 'Chennai',   626.8),
    ('Delhi',     'Mumbai',    1407.0),
    ('Delhi',     'Kolkata',   1472.0),
    ('Kolkata',   'Chennai',   1659.0),
    ('Bengaluru', 'Chennai',   350.2),
    ('Mumbai',    'Bengaluru', 980.5);
