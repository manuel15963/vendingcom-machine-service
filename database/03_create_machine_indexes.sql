-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- Índices (joins, filtros y búsqueda)
-- ============================================================

-- Filtros del listado de máquinas
CREATE INDEX IF NOT EXISTS idx_machines_customer   ON machines(customer_id);
CREATE INDEX IF NOT EXISTS idx_machines_location   ON machines(location_id);
CREATE INDEX IF NOT EXISTS idx_machines_status     ON machines(machine_status_id);
-- Búsqueda por código insensible a mayúsculas
CREATE INDEX IF NOT EXISTS idx_machines_code_lower ON machines (LOWER(code));

-- Eventos por máquina (ordenados por fecha)
CREATE INDEX IF NOT EXISTS idx_machine_events_machine ON machine_events(machine_id, event_date);
CREATE INDEX IF NOT EXISTS idx_machine_events_type    ON machine_events(event_type_id);

-- Documentos por máquina y tipo
CREATE INDEX IF NOT EXISTS idx_machine_documents_machine ON machine_documents(machine_id, document_type_id);

-- Auditoría y catálogos
CREATE INDEX IF NOT EXISTS idx_machine_audit_logs_machine     ON machine_audit_logs(machine_id);
CREATE INDEX IF NOT EXISTS idx_machine_audit_logs_action_date ON machine_audit_logs(action_type, executed_at);
CREATE INDEX IF NOT EXISTS idx_machine_parameters_group       ON machine_parameters(parameter_group, parameter_status, sort_order);
