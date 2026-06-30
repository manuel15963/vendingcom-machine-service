-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- Migración: reemplaza la columna libre `configuration` (JSONB) por campos
-- entendibles para el operador:
--   · machine_type_id            -> tipo de máquina (catálogo MACHINE_TYPE)
--   · maintenance_interval_days  -> mantenimiento preventivo (cada X días)
-- Idempotente: se puede correr varias veces sin error.
--
-- ORDEN DE DESPLIEGUE (importante):
--   1) Correr los pasos 1-3 de este archivo ANTES de desplegar el código nuevo
--      (el código nuevo lee/escribe estas columnas; el código viejo las ignora).
--   2) Desplegar el backend nuevo (push -> Render).
--   3) Una vez arriba el backend nuevo, correr el paso 4 (elimina `configuration`).
--      No correrlo antes: el backend viejo todavía escribe en esa columna.
-- ============================================================

-- 1) Nuevas columnas -----------------------------------------
ALTER TABLE machines ADD COLUMN IF NOT EXISTS machine_type_id           INTEGER;
ALTER TABLE machines ADD COLUMN IF NOT EXISTS maintenance_interval_days INTEGER;

-- 2) Restricciones (solo si aún no existen) ------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_machines_maint_interval') THEN
        ALTER TABLE machines ADD CONSTRAINT chk_machines_maint_interval
            CHECK (maintenance_interval_days IS NULL OR maintenance_interval_days > 0);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_machines_type') THEN
        ALTER TABLE machines ADD CONSTRAINT fk_machines_type
            FOREIGN KEY (machine_type_id) REFERENCES machine_parameters(parameter_id);
    END IF;
END $$;

-- 3) Catálogo de tipos de máquina (MACHINE_TYPE) -------------
INSERT INTO machine_parameters (parameter_group, parameter_code, parameter_value, description, sort_order, parameter_status) VALUES
('MACHINE_TYPE','SNACKS','Snacks','Máquina de snacks / golosinas.',1,1),
('MACHINE_TYPE','COLD_DRINKS','Bebidas frías','Máquina de bebidas frías (refrigerada).',2,1),
('MACHINE_TYPE','COFFEE','Café','Máquina de café / bebidas calientes.',3,1),
('MACHINE_TYPE','COMBO','Combinada','Snacks y bebidas en una sola máquina.',4,1),
('MACHINE_TYPE','OTHER','Otra','Otro tipo de máquina.',5,1)
ON CONFLICT (parameter_group, parameter_code) DO NOTHING;

-- 4) Limpieza: eliminar la columna libre antigua -------------
--    ⚠️ Correr SOLO después de que el backend nuevo esté desplegado.
-- ALTER TABLE machines DROP COLUMN IF EXISTS configuration;
