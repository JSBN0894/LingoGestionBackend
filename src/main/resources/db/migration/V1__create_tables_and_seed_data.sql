-- V1__create_tables_and_seed_data.sql
-- Migración inicial: Creación de tablas y datos semilla

-- ===========================================
-- TABLA DE ESTADOS (con soporte para estados anidados)
-- ===========================================
CREATE TABLE IF NOT EXISTS state (
    id BIGSERIAL PRIMARY KEY,
    parent_id BIGINT REFERENCES state(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL UNIQUE,
    description TEXT,
    type VARCHAR(50) NOT NULL DEFAULT 'OPERATION',  -- OPERATION, SHIPMENT, DELIVERY
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Índice para consultas por padre
CREATE INDEX IF NOT EXISTS idx_state_parent ON state(parent_id);
CREATE INDEX IF NOT EXISTS idx_state_type ON state(type);

-- Insertar estados principales y sus sub-estados
-- Estados principales (sin padre)
INSERT INTO state (name, description, type) VALUES
    ('Pendiente', 'La orden está pendiente de procesamiento', 'OPERATION'),
    ('Disponible', 'La orden está disponible para envío', 'OPERATION'),
    ('Enviado', 'La orden ha sido enviada', 'OPERATION'),
    ('Finalizado', 'La orden ha sido finalizada exitosamente', 'OPERATION'),
    ('Novedad', 'La orden presenta novedad que requiere atención', 'OPERATION');

-- Sub-estados de "Disponible" (id=2)
INSERT INTO state (parent_id, name, description, type) VALUES
    ((SELECT id FROM state WHERE name = 'Disponible'), 
     'Por reclamar el paquete', 'El paquete está listo para ser reclamado', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Disponible'), 
     'En proceso de entrega', 'El paquete está en proceso de entrega', 'SHIPMENT');

-- Sub-estados de "Enviado" (id=3)
INSERT INTO state (parent_id, name, description, type) VALUES
    ((SELECT id FROM state WHERE name = 'Enviado'), 
     'En camino hacia ti', 'El envío está en ruta hacia el destino', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Enviado'), 
     'Viajando a tu destino', 'El envío está en tránsito', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Enviado'), 
     'Tu envío fue devuelto', 'El envío ha sido devuelto al remitente', 'SHIPMENT');

-- Sub-estados de "Finalizado" (id=4)
INSERT INTO state (parent_id, name, description, type) VALUES
    ((SELECT id FROM state WHERE name = 'Finalizado'), 
     'Ya puedes recoger tu envío', 'El envío está disponible para recogida', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Finalizado'), 
     'En Centro Logístico de Tránsito', 'El envío está en centro de tránsito', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Finalizado'), 
     'Tu envío fue entregado', 'El envío ha sido entregado al destinatario', 'SHIPMENT'),
    ((SELECT id FROM state WHERE name = 'Finalizado'), 
     'En Centro Logístico Destino', 'El envío llegó al centro de destino', 'SHIPMENT');

-- Sub-estados de "Novedad" (id=5)
INSERT INTO state (parent_id, name, description, type) VALUES
    ((SELECT id FROM state WHERE name = 'Novedad'), 
     'Envío cancelado', 'El envío ha sido cancelado', 'SHIPMENT');

-- ===========================================
-- TABLA DE CATEGORÍAS
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
    category_id BIGINT REFERENCES categories(id),
    name VARCHAR(200) NOT NULL,
    price_per_unit BIGINT NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    image_url VARCHAR(500) NOT NULL,
    description TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLA DE CLIENTES
-- ===========================================
CREATE TABLE IF NOT EXISTS client (
    id_user VARCHAR(100) PRIMARY KEY,
    id_number VARCHAR(50) UNIQUE,
    name VARCHAR(200) NOT NULL,
    default_phone VARCHAR(20),
    default_city VARCHAR(100),
    default_address VARCHAR(200),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- TABLA DE ÓRDENES
-- ===========================================
CREATE TABLE IF NOT EXISTS orders (
    id BIGSERIAL PRIMARY KEY,
    client_id VARCHAR(100) NOT NULL REFERENCES client(id_user),
    operation_state_id BIGINT NOT NULL REFERENCES state(id),
    order_price BIGINT NOT NULL,
    order_address VARCHAR(200) NOT NULL,
    order_phone VARCHAR(20) NOT NULL,
    order_city VARCHAR(100) NOT NULL,
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
-- ÍNDICES PARA MEJORAR RENDIMIENTO
-- ===========================================
CREATE INDEX IF NOT EXISTS idx_products_category ON products(category_id);
CREATE INDEX IF NOT EXISTS idx_orders_client ON orders(client_id);
CREATE INDEX IF NOT EXISTS idx_orders_state ON orders(operation_state_id);
CREATE INDEX IF NOT EXISTS idx_client_id_number ON client(id_number);
