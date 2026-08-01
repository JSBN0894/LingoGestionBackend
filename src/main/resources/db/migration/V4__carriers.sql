-- V4__carriers.sql
-- Convierte Shipment.carrier de texto libre a un catalogo administrable.

CREATE TABLE IF NOT EXISTS carriers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(150) NOT NULL,
    contact_phone VARCHAR(50),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_carriers_name ON carriers (LOWER(name));

-- Migra los nombres de transportadora ya usados en envios existentes.
INSERT INTO carriers (name)
SELECT DISTINCT s.carrier
FROM shipments s
WHERE s.carrier IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM carriers c WHERE LOWER(c.name) = LOWER(s.carrier)
  );

-- Garantiza que el default historico ("Inter rapidisimo") exista como fila,
-- incluso en una base de datos nueva sin envios previos.
INSERT INTO carriers (name)
SELECT 'Inter rapidisimo'
WHERE NOT EXISTS (SELECT 1 FROM carriers WHERE LOWER(name) = LOWER('Inter rapidisimo'));

ALTER TABLE shipments ADD COLUMN IF NOT EXISTS carrier_id BIGINT;

UPDATE shipments s
SET carrier_id = c.id
FROM carriers c
WHERE LOWER(c.name) = LOWER(s.carrier)
  AND s.carrier_id IS NULL;

UPDATE shipments s
SET carrier_id = (SELECT id FROM carriers WHERE LOWER(name) = LOWER('Inter rapidisimo'))
WHERE s.carrier_id IS NULL;

ALTER TABLE shipments ALTER COLUMN carrier_id SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_shipment_carrier' AND table_name = 'shipments'
    ) THEN
        ALTER TABLE shipments
            ADD CONSTRAINT fk_shipment_carrier FOREIGN KEY (carrier_id) REFERENCES carriers(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_shipments_carrier ON shipments(carrier_id);

-- Se conserva el texto original por trazabilidad, pero deja de ser la fuente
-- de verdad: la app ahora lee/escribe carrier_id.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'shipments' AND column_name = 'carrier'
    ) THEN
        ALTER TABLE shipments RENAME COLUMN carrier TO carrier_legacy_name;
        ALTER TABLE shipments ALTER COLUMN carrier_legacy_name DROP NOT NULL;
        ALTER TABLE shipments ALTER COLUMN carrier_legacy_name DROP DEFAULT;
    END IF;
END $$;
