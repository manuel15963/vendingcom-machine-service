-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- Triggers: updated_at automático y code autogenerado (VEND-000001)
-- ============================================================

-- ------------------------------------------------------------
-- 1) updated_at automático en cada UPDATE
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_machine_set_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_machines_updated_at ON machines;
CREATE TRIGGER trg_machines_updated_at BEFORE UPDATE ON machines
    FOR EACH ROW EXECUTE FUNCTION fn_machine_set_updated_at();

DROP TRIGGER IF EXISTS trg_machine_parameters_updated_at ON machine_parameters;
CREATE TRIGGER trg_machine_parameters_updated_at BEFORE UPDATE ON machine_parameters
    FOR EACH ROW EXECUTE FUNCTION fn_machine_set_updated_at();

-- ------------------------------------------------------------
-- 2) code autogenerado (VEND-000001) + qr_code por defecto = code
--    (usa la secuencia seq_machine_code creada en 01_create_machine_tables.sql)
-- ------------------------------------------------------------
CREATE OR REPLACE FUNCTION fn_machine_set_code()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.code IS NULL OR NEW.code = '' THEN
        NEW.code = 'VEND-' || LPAD(nextval('seq_machine_code')::TEXT, 6, '0');
    END IF;
    IF NEW.qr_code IS NULL OR NEW.qr_code = '' THEN
        NEW.qr_code = NEW.code;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_machines_set_code ON machines;
CREATE TRIGGER trg_machines_set_code BEFORE INSERT ON machines
    FOR EACH ROW EXECUTE FUNCTION fn_machine_set_code();
