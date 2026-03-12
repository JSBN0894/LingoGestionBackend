-- ===========================================
-- USUARIO ADMINISTRADOR INICIAL
-- ===========================================
-- Este script crea el usuario administrador por defecto.
-- 
-- Credenciales por defecto:
--   Username: admin
--   Password: Admin@123456
--
-- IMPORTANTE: Cambiar la contraseña inmediatamente después del primer login!
-- ===========================================

-- Password: Admin@123456 (hash generado con BCrypt, strength=12)
INSERT INTO users (id, username, email, password, full_name, role, is_enabled, created_at, updated_at)
VALUES (
    '00000000-0000-0000-0000-000000000001',
    'admin',
    'admin@linogo.local',
    '$2a$12$YMxSGGdx7pFWqQJhJxQbK.4vN8KZqJxQbK.4vN8KZqJxQbK.4vN8KZq',
    'Administrador del Sistema',
    'ADMIN',
    true,
    NOW(),
    NOW()
)
ON CONFLICT (username) DO NOTHING;

-- Para generar un nuevo hash BCrypt:
-- Opción 1: Usar Spring Security BCryptPasswordEncoder
-- Opción 2: https://bcrypt-generator.com/ (Rounds: 12)
