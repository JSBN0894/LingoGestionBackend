-- V3__baseline_shipping_and_auth_tables.sql
-- Estas tablas (envios, estados de envio, historial de tracking, usuarios,
-- refresh tokens) nunca tuvieron una migracion propia: existen en produccion
-- porque Hibernate las creo directamente con ddl-auto=update. Esta migracion
-- las declara explicitamente (con IF NOT EXISTS) para que Flyway tome el
-- control del esquema completo sin alterar lo que ya esta desplegado.

-- ===========================================
-- ESTADOS DE ENVIO (sub-estado ligado a un estado de operacion)
-- ===========================================
CREATE TABLE IF NOT EXISTS shipment_states (
    id BIGSERIAL PRIMARY KEY,
    state_id BIGINT NOT NULL REFERENCES states(id),
    name VARCHAR(150) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipment_states_state ON shipment_states(state_id);

-- ===========================================
-- ENVIOS
-- ===========================================
CREATE TABLE IF NOT EXISTS shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    shipping_state_id BIGINT NOT NULL REFERENCES shipment_states(id),
    carrier VARCHAR(150) NOT NULL DEFAULT 'Inter rapidisimo',
    is_cash_on_delivery BOOLEAN NOT NULL DEFAULT true,
    shipping_cost BIGINT,
    estimate_delivery_date TIMESTAMP,
    weight BIGINT,
    guide_number VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipments_state ON shipments(shipping_state_id);

-- ===========================================
-- HISTORIAL DE SEGUIMIENTO DE ENVIOS
-- ===========================================
CREATE TABLE IF NOT EXISTS shipment_tracking_history (
    id BIGSERIAL PRIMARY KEY,
    shipment_id BIGINT NOT NULL,
    status_from VARCHAR(150),
    status_to VARCHAR(150) NOT NULL,
    changed_by VARCHAR(150),
    notes TEXT,
    changed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_shipment_tracking_shipment ON shipment_tracking_history(shipment_id);

-- ===========================================
-- USUARIOS (login del panel/movil)
-- ===========================================
CREATE TABLE IF NOT EXISTS users (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(255) NOT NULL,
    role VARCHAR(30) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- REFRESH TOKENS
-- ===========================================
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL UNIQUE REFERENCES users(id),
    token VARCHAR(500) NOT NULL UNIQUE,
    expiry_date TIMESTAMP NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
