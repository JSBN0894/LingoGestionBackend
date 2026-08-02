-- V1__create_tables_and_seed_data.sql
-- Migracion inicial: creacion de tablas base.
--
-- NOTA (2026): esta migracion nunca se habia ejecutado en ningun entorno real
-- porque spring.flyway.enabled estaba en false y el esquema lo generaba
-- Hibernate solo (ddl-auto=update). El archivo original quedo desincronizado
-- de las entidades reales (nombres de tabla, columnas faltantes) y por eso
-- se corrigio aqui para que coincida con el esquema que ya existe en
-- produccion, en vez de intentar crear uno distinto. Todas las sentencias
-- usan IF NOT EXISTS: si la tabla ya existe (como en produccion), esta
-- migracion no hace nada mas que registrar el baseline en Flyway.
--
-- Los datos semilla (estados, categorias, usuario admin, clientes demo) ya
-- NO se insertan aqui: los maneja `DataInitializer` (CommandLineRunner) en
-- el arranque de la aplicacion, que es lo que realmente se ha usado hasta
-- ahora. Duplicar la siembra en SQL y en codigo generaba riesgo de datos
-- inconsistentes (nombres de estado con distinta capitalizacion).

-- ===========================================
-- TABLA DE ESTADOS
-- ===========================================
CREATE TABLE IF NOT EXISTS states (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    priority INTEGER NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'OPERATION',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_states_type ON states(type);

-- ===========================================
-- TABLA DE CATEGORIAS
-- ===========================================
CREATE TABLE IF NOT EXISTS categories (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES categories(id),
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLA DE PRODUCTOS
-- ===========================================
CREATE TABLE IF NOT EXISTS products (
    id BIGSERIAL PRIMARY KEY,
    category_id BIGINT NOT NULL REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    price_per_unit BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLA DE CLIENTES (clave primaria = cedula)
-- ===========================================
CREATE TABLE IF NOT EXISTS customers (
    cedula BIGINT PRIMARY KEY,
    name VARCHAR(200) NOT NULL
);

CREATE TABLE IF NOT EXISTS customer_phones (
    customer_cedula BIGINT NOT NULL REFERENCES customers(cedula) ON DELETE CASCADE,
    phone VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS customer_addresses (
    customer_cedula BIGINT NOT NULL REFERENCES customers(cedula) ON DELETE CASCADE,
    address VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_customer_phones_cedula ON customer_phones(customer_cedula);
CREATE INDEX IF NOT EXISTS idx_customer_addresses_cedula ON customer_addresses(customer_cedula);

-- ===========================================
-- TABLA DE ORDENES
-- ===========================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    customer_id BIGINT NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    operation_state_id BIGINT NOT NULL REFERENCES states(id),
    order_price BIGINT NOT NULL,
    order_address VARCHAR(200) NOT NULL,
    order_phone VARCHAR(20) NOT NULL,
    order_city VARCHAR(100) NOT NULL,
    observation VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLA INTERMEDIA ORDER_PRODUCT
-- ===========================================
CREATE TABLE IF NOT EXISTS order_product (
    order_id BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES products(id),
    quantity INT NOT NULL,
    price BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (order_id, product_id)
);

-- ===========================================
-- INDICES PARA MEJORAR RENDIMIENTO
-- ===========================================
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_state ON orders(operation_state_id);
