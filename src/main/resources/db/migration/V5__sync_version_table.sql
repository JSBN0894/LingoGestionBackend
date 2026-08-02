-- V5__sync_version_table.sql
-- La tabla sync_version nunca tuvo migracion propia (igual que las tablas
-- descritas en V3): existia en los entornos ya desplegados porque Hibernate
-- la creo con ddl-auto=update antes de que Flyway tomara el control del
-- esquema. Sin esta migracion, un entorno construido desde cero (por
-- ejemplo tras recrear el volumen de Postgres) falla en el arranque porque
-- import.sql y la entidad SyncVersion la requieren.

CREATE TABLE IF NOT EXISTS sync_version (
    id BIGINT PRIMARY KEY,
    version BIGINT NOT NULL,
    description VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);
