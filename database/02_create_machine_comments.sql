-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- DICCIONARIO DE DATOS — descripción de cada tabla y cada campo
-- (ejecutar después de 01_create_machine_tables.sql)
-- ============================================================

-- ------------------------------------------------------------
-- machine_parameters
-- ------------------------------------------------------------
COMMENT ON TABLE  machine_parameters                  IS 'Catálogo de estados y tipos del módulo de máquinas (MACHINE_STATUS, MACHINE_TYPE, EVENT_TYPE, DOCUMENT_TYPE).';
COMMENT ON COLUMN machine_parameters.parameter_id     IS 'Identificador único del parámetro (clave primaria).';
COMMENT ON COLUMN machine_parameters.parameter_group  IS 'Grupo al que pertenece el parámetro. Ej: MACHINE_STATUS, MACHINE_TYPE, EVENT_TYPE, DOCUMENT_TYPE.';
COMMENT ON COLUMN machine_parameters.parameter_code   IS 'Código técnico del parámetro dentro del grupo. Ej: ACTIVE, INSTALLATION, MANUAL.';
COMMENT ON COLUMN machine_parameters.parameter_value  IS 'Valor o etiqueta legible del parámetro (Activa, Instalación...).';
COMMENT ON COLUMN machine_parameters.description      IS 'Descripción legible de para qué sirve el parámetro.';
COMMENT ON COLUMN machine_parameters.sort_order       IS 'Orden de aparición en listas y combos del frontend.';
COMMENT ON COLUMN machine_parameters.parameter_status IS 'Estado del parámetro: 0 = inactivo, 1 = activo.';
COMMENT ON COLUMN machine_parameters.created_at       IS 'Fecha y hora en que se creó el parámetro.';
COMMENT ON COLUMN machine_parameters.updated_at       IS 'Fecha y hora de la última modificación del parámetro.';

-- ------------------------------------------------------------
-- machines
-- ------------------------------------------------------------
COMMENT ON TABLE  machines                       IS 'Máquinas vending (activo físico). El inventario va en product-inventory-service y el dinero en collection-service.';
COMMENT ON COLUMN machines.machine_id            IS 'Identificador único de la máquina (clave primaria).';
COMMENT ON COLUMN machines.code                  IS 'Código de negocio autogenerado por trigger: VEND-000001. Único e inmutable.';
COMMENT ON COLUMN machines.qr_code               IS 'Valor del código QR (único). Por defecto = code; puede ser una URL o token.';
COMMENT ON COLUMN machines.customer_id           IS 'ID del cliente dueño (customer-service). No es FK: pertenece a otro microservicio (se valida por HTTP).';
COMMENT ON COLUMN machines.location_id           IS 'ID de la ubicación donde está instalada (location-service). No es FK: se valida por HTTP.';
COMMENT ON COLUMN machines.model                 IS 'Modelo de la máquina.';
COMMENT ON COLUMN machines.brand                 IS 'Marca de la máquina.';
COMMENT ON COLUMN machines.serial_number         IS 'Número de serie de fábrica (único).';
COMMENT ON COLUMN machines.machine_status_id     IS 'Estado actual de la máquina. FK a machine_parameters (grupo MACHINE_STATUS).';
COMMENT ON COLUMN machines.installation_date     IS 'Fecha de instalación de la máquina.';
COMMENT ON COLUMN machines.last_maintenance_date IS 'Fecha del último mantenimiento.';
COMMENT ON COLUMN machines.machine_type_id           IS 'Tipo/categoría de la máquina (hardware). FK a machine_parameters (grupo MACHINE_TYPE). Opcional.';
COMMENT ON COLUMN machines.maintenance_interval_days IS 'Cada cuántos días requiere mantenimiento preventivo. Con la última fecha el sistema calcula y avisa el próximo. Opcional.';
COMMENT ON COLUMN machines.notes                 IS 'Observaciones adicionales.';
COMMENT ON COLUMN machines.version               IS 'Bloqueo optimista para evitar sobrescrituras concurrentes.';
COMMENT ON COLUMN machines.created_by_user_id    IS 'ID del usuario de auth-service que registró la máquina (no es FK).';
COMMENT ON COLUMN machines.updated_by_user_id    IS 'ID del usuario de auth-service que modificó la máquina por última vez (no es FK).';
COMMENT ON COLUMN machines.created_at            IS 'Fecha y hora en que se registró la máquina.';
COMMENT ON COLUMN machines.updated_at            IS 'Fecha y hora de la última modificación de la máquina.';

-- ------------------------------------------------------------
-- machine_events
-- ------------------------------------------------------------
COMMENT ON TABLE  machine_events                      IS 'Eventos importantes de la máquina (instalación, mantenimiento, reparación, cambios).';
COMMENT ON COLUMN machine_events.event_id             IS 'Identificador único del evento (clave primaria).';
COMMENT ON COLUMN machine_events.machine_id           IS 'Máquina a la que pertenece el evento. FK a machines (ON DELETE CASCADE).';
COMMENT ON COLUMN machine_events.event_type_id        IS 'Tipo de evento. FK a machine_parameters (grupo EVENT_TYPE).';
COMMENT ON COLUMN machine_events.title                IS 'Título corto del evento.';
COMMENT ON COLUMN machine_events.description          IS 'Descripción detallada del evento.';
COMMENT ON COLUMN machine_events.performed_by_user_id IS 'ID del usuario de auth-service que realizó el evento (no es FK).';
COMMENT ON COLUMN machine_events.event_date           IS 'Fecha y hora en que ocurrió el evento.';
COMMENT ON COLUMN machine_events.metadata             IS 'Datos adicionales del evento en formato JSON.';
COMMENT ON COLUMN machine_events.created_at           IS 'Fecha y hora de registro del evento.';

-- ------------------------------------------------------------
-- machine_documents
-- ------------------------------------------------------------
COMMENT ON TABLE  machine_documents                     IS 'Documentos asociados a la máquina (manual, garantía, ficha técnica, fotos). El archivo vive en S3/MinIO; aquí solo la URL.';
COMMENT ON COLUMN machine_documents.document_id         IS 'Identificador único del documento (clave primaria).';
COMMENT ON COLUMN machine_documents.machine_id          IS 'Máquina a la que pertenece el documento. FK a machines (ON DELETE CASCADE).';
COMMENT ON COLUMN machine_documents.document_type_id    IS 'Tipo de documento. FK a machine_parameters (grupo DOCUMENT_TYPE).';
COMMENT ON COLUMN machine_documents.file_name           IS 'Nombre del archivo.';
COMMENT ON COLUMN machine_documents.file_url            IS 'URL del archivo en el storage externo (S3/MinIO).';
COMMENT ON COLUMN machine_documents.file_size           IS 'Tamaño del archivo en bytes.';
COMMENT ON COLUMN machine_documents.mime_type           IS 'Tipo MIME del archivo (application/pdf, image/png, etc.).';
COMMENT ON COLUMN machine_documents.uploaded_by_user_id IS 'ID del usuario de auth-service que subió el documento (no es FK).';
COMMENT ON COLUMN machine_documents.uploaded_at         IS 'Fecha y hora de subida del documento.';

-- ------------------------------------------------------------
-- machine_audit_logs
-- ------------------------------------------------------------
COMMENT ON TABLE  machine_audit_logs                     IS 'Auditoría de cambios del módulo de máquinas. Solo se inserta, no se modifica (append-only).';
COMMENT ON COLUMN machine_audit_logs.audit_log_id        IS 'Identificador único del evento de auditoría (clave primaria).';
COMMENT ON COLUMN machine_audit_logs.machine_id          IS 'Máquina afectada por la acción (puede ser NULL si no aplica). FK a machines.';
COMMENT ON COLUMN machine_audit_logs.affected_table_name IS 'Nombre de la tabla afectada (machines, machine_events, etc.).';
COMMENT ON COLUMN machine_audit_logs.affected_record_id  IS 'ID del registro afectado dentro de la tabla indicada.';
COMMENT ON COLUMN machine_audit_logs.action_type         IS 'Tipo de acción auditada. Ej: MACHINE_CREATED, MACHINE_UPDATED, STATUS_CHANGED.';
COMMENT ON COLUMN machine_audit_logs.action_description  IS 'Descripción legible de la acción realizada.';
COMMENT ON COLUMN machine_audit_logs.old_data            IS 'Estado del registro ANTES del cambio, en formato JSON.';
COMMENT ON COLUMN machine_audit_logs.new_data            IS 'Estado del registro DESPUÉS del cambio, en formato JSON.';
COMMENT ON COLUMN machine_audit_logs.ip_address          IS 'Dirección IP desde donde se ejecutó la acción.';
COMMENT ON COLUMN machine_audit_logs.user_agent          IS 'Navegador o cliente desde donde se ejecutó la acción.';
COMMENT ON COLUMN machine_audit_logs.executed_by_user_id IS 'ID del usuario de auth-service que ejecutó la acción (no es FK).';
COMMENT ON COLUMN machine_audit_logs.executed_at         IS 'Fecha y hora en que ocurrió la acción auditada.';
