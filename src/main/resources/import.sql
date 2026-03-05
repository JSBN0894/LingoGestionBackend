-- ===========================================
-- SCRIPT DE INICIALIZACIÓN DE DATOS
-- ===========================================
-- Este script se ejecuta al iniciar la aplicación con spring.sql.init.mode=always
-- Usamos INSERT ... ON CONFLICT para evitar errores si los datos ya existen

-- ===========================================
-- 1. VERSIÓN DE SINCRONIZACIÓN
-- ===========================================
INSERT INTO sync_version (id, version, description, created_at, updated_at)
VALUES (1, 1, 'Versión inicial del catálogo', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- 2. CATEGORÍAS DE PRODUCTOS
-- ===========================================
INSERT INTO categories (id, name, "parentId", created_at, updated_at)
VALUES
    (1, 'Moldes', NULL, NOW(), NOW()),
    (2, 'Accesorios', NULL, NOW(), NOW()),
    (3, 'Moldes de silicona', 1, NOW(), NOW()),
    (4, 'Moldes de metal', 1, NOW(), NOW()),
    (5, 'Herramientas', NULL, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- 3. ESTADOS DE OPERACIONES (states)
-- ===========================================
INSERT INTO states (id, name, type, priority, created_at, updated_at)
VALUES
    -- Estados de operaciones
    (1, 'Pendiente', 'OPERATION', 1, NOW(), NOW()),
    (2, 'Confirmado', 'OPERATION', 2, NOW(), NOW()),
    (3, 'En preparación', 'OPERATION', 3, NOW(), NOW()),
    (4, 'Enviado', 'OPERATION', 4, NOW(), NOW()),
    (5, 'Entregado', 'OPERATION', 5, NOW(), NOW()),
    (6, 'Cancelado', 'OPERATION', 6, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- 4. ESTADOS DE ENVÍOS (shipment_states)
-- ===========================================
INSERT INTO shipment_states (id, name, state_id, created_at, updated_at)
VALUES
    (1, 'Por enviar', 1, NOW(), NOW()),
    (2, 'En tránsito', 4, NOW(), NOW()),
    (3, 'En reparto', 4, NOW(), NOW()),
    (4, 'Entregado', 5, NOW(), NOW()),
    (5, 'Fallido', 6, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- 5. USUARIOS POR DEFECTO
-- ===========================================
-- Contraseñas hasheadas con BCrypt:
-- admin: admin123
-- user: user123
-- NOTA: Cambiar contraseñas en producción inmediatamente

INSERT INTO users (id, username, email, password, full_name, role, is_enabled, created_at, updated_at)
VALUES
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'admin',
        'admin@linogo.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzS3MebAJu',
        'Administrador del Sistema',
        'ADMIN',
        true,
        NOW(),
        NOW()
    ),
    (
        'b1eebc99-9c0b-4ef8-bb6d-6bb9bd380a22',
        'user',
        'user@linogo.com',
        '$2a$12$3JkYkNxUeQ7h9fGzLqXwO.qZ8vN5xJ9K2L4M6P8R0S2T4U6V8W0Y2',
        'Usuario Demo',
        'USER',
        true,
        NOW(),
        NOW()
    )
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- 6. PRODUCTOS DE EJEMPLO
-- ===========================================
INSERT INTO products (id, name, price_per_unit, stock, description, category_id, image_url, created_at, updated_at)
VALUES
    (1001, 'Molde Redondo Silicona 15cm', 1500, 50, 'Molde redondo de silicona antiadherente, 15cm diámetro', 3, 'https://via.placeholder.com/300x300?text=Molde+Redondo', NOW(), NOW()),
    (1002, 'Molde Cuadrado Silicona 20cm', 2000, 30, 'Molde cuadrado de silicona para hornear, 20cm', 3, 'https://via.placeholder.com/300x300?text=Molde+Cuadrado', NOW(), NOW()),
    (1003, 'Molde Metal Redondo 25cm', 1200, 40, 'Molde metálico antiadherente profesional, 25cm', 4, 'https://via.placeholder.com/300x300?text=Molde+Metal', NOW(), NOW()),
    (1004, 'Set Espátulas Silicona x3', 800, 100, 'Set de 3 espátulas de silicona resistente al calor', 2, 'https://via.placeholder.com/300x300?text=Espatulas', NOW(), NOW()),
    (1005, 'Molde Corazón Silicona', 1800, 25, 'Molde con forma de corazón para repostería', 3, 'https://via.placeholder.com/300x300?text=Molde+Corazon', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

