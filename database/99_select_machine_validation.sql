-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- Consultas de verificación (¿se creó / insertó todo bien?)
-- ============================================================

-- 1) Catálogo: parámetros por grupo  (esperado: MACHINE_STATUS 4, EVENT_TYPE 6, DOCUMENT_TYPE 5)
SELECT parameter_group, COUNT(*) AS total
FROM machine_parameters
GROUP BY parameter_group
ORDER BY parameter_group;

-- 2) Máquinas con su estado legible  (code/qr_code autogenerados)
SELECT m.machine_id, m.code, m.qr_code, m.serial_number,
       m.customer_id, m.location_id, m.brand, m.model,
       p.parameter_value AS estado,
       m.created_at
FROM machines m
JOIN machine_parameters p ON p.parameter_id = m.machine_status_id
ORDER BY m.machine_id;

-- 3) Eventos por máquina (con tipo legible)
SELECT m.code AS maquina, p.parameter_value AS tipo_evento, e.title, e.event_date
FROM machine_events e
JOIN machines m            ON m.machine_id   = e.machine_id
JOIN machine_parameters p  ON p.parameter_id = e.event_type_id
ORDER BY m.code, e.event_date;

-- 4) Documentos por máquina (con tipo legible)
SELECT m.code AS maquina, p.parameter_value AS tipo_documento,
       d.file_name, d.mime_type, d.file_size
FROM machine_documents d
JOIN machines m            ON m.machine_id   = d.machine_id
JOIN machine_parameters p  ON p.parameter_id = d.document_type_id
ORDER BY m.code, d.file_name;

-- 5) Auditoría
SELECT m.code AS maquina, a.action_type, a.action_description, a.new_data, a.executed_at
FROM machine_audit_logs a
LEFT JOIN machines m ON m.machine_id = a.machine_id
ORDER BY a.executed_at;

-- 6) Resumen general: filas por tabla
SELECT 'machine_parameters' AS tabla, COUNT(*) AS filas FROM machine_parameters
UNION ALL SELECT 'machines',           COUNT(*) FROM machines
UNION ALL SELECT 'machine_events',     COUNT(*) FROM machine_events
UNION ALL SELECT 'machine_documents',  COUNT(*) FROM machine_documents
UNION ALL SELECT 'machine_audit_logs', COUNT(*) FROM machine_audit_logs;
