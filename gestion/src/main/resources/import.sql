-- Versión inicial de sincronización
INSERT INTO sync_version (id, version, description, created_at, updated_at)
VALUES (1, 1, 'Versión inicial del catálogo', NOW())
ON CONFLICT (id) DO NOTHING;

-- Categorías de ejemplo
INSERT INTO categories (id, name, "parentId", created_at, updated_at)
VALUES 
    (1, 'Moldes', NULL, NOW(), NOW()),
    (2, 'Accesorios', NULL, NOW(), NOW()),
    (3, 'Moldes de silicona', 1, NOW(), NOW()),
    (4, 'Moldes de metal', 1, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Estados de operación
INSERT INTO states (id, name, priority, created_at, updated_at)
VALUES 
    (1, 'Pendiente', 1, NOW(), NOW()),
    (2, 'Confirmado', 2, NOW(), NOW()),
    (3, 'En preparación', 3, NOW(), NOW()),
    (4, 'Enviado', 4, NOW(), NOW()),
    (5, 'Entregado', 5, NOW(), NOW()),
    (6, 'Cancelado', 6, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Estados base para envíos
INSERT INTO states (id, name, priority, created_at, updated_at)
VALUES 
    (10, 'Por enviar', 1, NOW(), NOW()),
    (11, 'En tránsito', 2, NOW(), NOW()),
    (12, 'En reparto', 3, NOW(), NOW()),
    (13, 'Entregado', 4, NOW(), NOW()),
    (14, 'Fallido', 5, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- Estados de envío (shipment_states)
INSERT INTO shipment_states (id, name, state_id, created_at, updated_at)
VALUES
    (1, 'Por enviar', 10, NOW(), NOW()),
    (2, 'En tránsito', 11, NOW(), NOW()),
    (3, 'En reparto', 12, NOW(), NOW()),
    (4, 'Entregado', 13, NOW(), NOW()),
    (5, 'Fallido', 14, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

-- ===========================================
-- USUARIOS POR DEFECTO (contraseñas hasheadas con BCrypt)
-- ===========================================
-- Admin: admin123 (hash BCrypt)
-- User: user123 (hash BCrypt)
-- NOTA: En producción, usar contraseñas únicas y seguras

INSERT INTO users (id, username, email, password, full_name, role, is_enabled, created_at, updated_at)
VALUES 
    (
        'a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11',
        'admin',
        'admin@linogo.com',
        '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5GyYzS3MebAJu',
        'Administrador',
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
