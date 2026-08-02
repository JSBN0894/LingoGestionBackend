-- V6__roles_and_permissions.sql
-- Reemplaza el sistema de roles fijos (enum Role en Kotlin) por roles
-- dinamicos con permisos asignables desde la plataforma. roles/
-- role_permissions/user_roles son tablas nuevas (nunca existieron via
-- Hibernate ddl-auto, a diferencia de las tablas que V3 tuvo que declarar
-- con IF NOT EXISTS) asi que se crean sin guardas.

-- ===========================================
-- ROLES
-- ===========================================
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ===========================================
-- PERMISOS POR ROL
-- permission_code es un string validado en Kotlin (enum Permission), no una
-- FK: el catalogo de permisos vive en codigo, no en la base de datos.
-- ===========================================
CREATE TABLE role_permissions (
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission_code)
);

-- ===========================================
-- USUARIOS <-> ROLES (muchos a muchos)
-- ===========================================
CREATE TABLE user_roles (
    user_id VARCHAR(36) NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- ===========================================
-- MIGRACION DE DATOS EXISTENTES
-- Crea un Role por cada valor distinto que exista hoy en users.role (0 filas
-- si la tabla users esta vacia — entorno fresco, el rol admin inicial lo
-- crea DataInitializer al arrancar), con los permisos que ese rol otorgaba
-- implicitamente bajo las anotaciones @AdminOnly/@VentasOnly/
-- @ProduccionOnly/@LogisticaOnly/@VentasOrLogisticaOnly, y enlaza cada
-- usuario existente a su rol migrado.
-- ===========================================

-- 1) Un Role por cada valor distinto presente en users.role
INSERT INTO roles (name, description)
SELECT DISTINCT role, 'Rol migrado automaticamente desde el sistema de roles fijos anterior'
FROM users
ON CONFLICT (name) DO NOTHING;

-- 2) Permisos del rol migrado "ADMIN" (equivalente a superusuario: estaba
--    incluido en TODOS los hasRole/hasAnyRole del sistema anterior)
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code
FROM roles r
JOIN (VALUES
    ('USERS_MANAGE'), ('AUDIT_VIEW'), ('ROLES_MANAGE'), ('DASHBOARD_VIEW'), ('SYSTEM_DOCS_VIEW'),
    ('CATEGORIES_MANAGE'), ('STATES_MANAGE'), ('SHIPMENT_STATES_MANAGE'),
    ('CARRIERS_MANAGE'), ('CARRIERS_DELETE'),
    ('CUSTOMERS_MANAGE'), ('CUSTOMERS_DELETE'),
    ('ORDERS_MANAGE'), ('ORDERS_DELETE'), ('ORDERS_SHIP'),
    ('PRODUCTS_MANAGE'),
    ('SHIPMENTS_MANAGE'), ('SHIPMENTS_DELETE')
) AS p(code) ON true
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 3) Permisos del rol migrado "VENTAS" (VentasOnly + VentasOrLogisticaOnly)
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code
FROM roles r
JOIN (VALUES ('CUSTOMERS_MANAGE'), ('ORDERS_MANAGE'), ('ORDERS_SHIP'), ('DASHBOARD_VIEW')) AS p(code) ON true
WHERE r.name = 'VENTAS'
ON CONFLICT DO NOTHING;

-- 4) Permisos del rol migrado "PRODUCCION" (ProduccionOnly)
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code
FROM roles r
JOIN (VALUES ('PRODUCTS_MANAGE')) AS p(code) ON true
WHERE r.name = 'PRODUCCION'
ON CONFLICT DO NOTHING;

-- 5) Permisos del rol migrado "LOGISTICA" (LogisticaOnly + VentasOrLogisticaOnly)
INSERT INTO role_permissions (role_id, permission_code)
SELECT r.id, p.code
FROM roles r
JOIN (VALUES ('CARRIERS_MANAGE'), ('SHIPMENTS_MANAGE'), ('ORDERS_SHIP')) AS p(code) ON true
WHERE r.name = 'LOGISTICA'
ON CONFLICT DO NOTHING;

-- 6) "USER" y "SELLER" (rol muerto, nunca asignado en la practica) no
--    otorgaban ningun permiso elevado bajo el sistema anterior: se migran
--    a roles sin ningun permiso (el rol queda creado, vacio, por si algun
--    usuario ya lo tenia, para no dejarlo sin ningun rol).

-- 7) Enlazar cada usuario existente a su rol migrado
INSERT INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM users u
JOIN roles r ON r.name = u.role
ON CONFLICT DO NOTHING;

-- ===========================================
-- Eliminar la columna de rol fijo
-- ===========================================
ALTER TABLE users DROP COLUMN IF EXISTS role;
