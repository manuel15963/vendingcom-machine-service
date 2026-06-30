-- ============================================================
-- VENDING.COM S.A.C  —  MACHINE-SERVICE
-- Base de datos: vendingcom_db  (LA MISMA que usan auth / customer / location)
-- Módulo: MACHINE - Gestión de máquinas vending  (PostgreSQL / Supabase)
-- ------------------------------------------------------------
-- Comparte la base (mismo Postgres/Supabase), separada por prefijo de
-- tabla (machine_*). NO se crea la base aquí.
-- Orden: parameters primero (las demás lo referencian).
-- La máquina es solo el ACTIVO FÍSICO. customer_id y location_id vienen de
-- otros microservicios (SIN FK física).
-- ============================================================

-- ------------------------------------------------------------
-- 1) CATÁLOGO DE PARÁMETROS
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS machine_parameters (
    parameter_id      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    parameter_group   VARCHAR(50)  NOT NULL,
    parameter_code    VARCHAR(50)  NOT NULL,
    parameter_value   VARCHAR(100) NOT NULL,
    description       VARCHAR(255),
    sort_order        INTEGER      NOT NULL DEFAULT 1,
    parameter_status  SMALLINT     NOT NULL DEFAULT 1,   -- 0=INACTIVO, 1=ACTIVO
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP,
    CONSTRAINT uq_machine_parameters_group_code UNIQUE (parameter_group, parameter_code),
    CONSTRAINT chk_machine_parameters_status CHECK (parameter_status IN (0,1))
);

-- ------------------------------------------------------------
-- 2) MÁQUINAS  (tabla principal)
-- ------------------------------------------------------------
CREATE SEQUENCE IF NOT EXISTS seq_machine_code START 1;

CREATE TABLE IF NOT EXISTS machines (
    machine_id            INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code                  VARCHAR(50)  NOT NULL,   -- VEND-000001 (autogenerado por trigger)
    qr_code               VARCHAR(255) NOT NULL,   -- valor del QR (por defecto = code)
    customer_id           INTEGER      NOT NULL,   -- cliente dueño (customer-service, no FK)
    location_id           INTEGER      NOT NULL,   -- ubicación instalada (location-service, no FK)
    model                 VARCHAR(100),
    brand                 VARCHAR(100),
    serial_number         VARCHAR(100),
    machine_status_id     INTEGER      NOT NULL,   -- FK -> machine_parameters (MACHINE_STATUS)
    installation_date     DATE,
    last_maintenance_date DATE,
    machine_type_id       INTEGER,                     -- FK -> machine_parameters (MACHINE_TYPE); opcional
    maintenance_interval_days INTEGER,                 -- cada cuántos días requiere mantenimiento preventivo
    notes                 TEXT,
    version               INTEGER      NOT NULL DEFAULT 0,  -- bloqueo optimista (lo maneja la app)
    created_by_user_id    INTEGER,                 -- usuario de auth-service (no FK)
    updated_by_user_id    INTEGER,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP,
    CONSTRAINT uq_machines_code    UNIQUE (code),
    CONSTRAINT uq_machines_qr_code UNIQUE (qr_code),
    CONSTRAINT uq_machines_serial  UNIQUE (serial_number),
    CONSTRAINT chk_machines_maint_interval CHECK (maintenance_interval_days IS NULL OR maintenance_interval_days > 0),
    CONSTRAINT fk_machines_status  FOREIGN KEY (machine_status_id) REFERENCES machine_parameters(parameter_id),
    CONSTRAINT fk_machines_type    FOREIGN KEY (machine_type_id)   REFERENCES machine_parameters(parameter_id)
);

-- ------------------------------------------------------------
-- 3) EVENTOS DE LA MÁQUINA
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS machine_events (
    event_id             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    machine_id           INTEGER      NOT NULL,
    event_type_id        INTEGER      NOT NULL,   -- FK -> machine_parameters (EVENT_TYPE)
    title                VARCHAR(150) NOT NULL,
    description          TEXT,
    performed_by_user_id INTEGER,                 -- usuario de auth-service (no FK)
    event_date           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    metadata             JSONB,
    created_at           TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_machine_events_machine FOREIGN KEY (machine_id)    REFERENCES machines(machine_id) ON DELETE CASCADE,
    CONSTRAINT fk_machine_events_type    FOREIGN KEY (event_type_id) REFERENCES machine_parameters(parameter_id)
);

-- ------------------------------------------------------------
-- 4) DOCUMENTOS DE LA MÁQUINA  (archivo en S3/MinIO; aquí solo la URL)
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS machine_documents (
    document_id          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    machine_id           INTEGER      NOT NULL,
    document_type_id     INTEGER      NOT NULL,   -- FK -> machine_parameters (DOCUMENT_TYPE)
    file_name            VARCHAR(255) NOT NULL,
    file_url             VARCHAR(500) NOT NULL,
    file_size            BIGINT,
    mime_type            VARCHAR(100),
    uploaded_by_user_id  INTEGER,                 -- usuario de auth-service (no FK)
    uploaded_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_machine_documents_machine FOREIGN KEY (machine_id)       REFERENCES machines(machine_id) ON DELETE CASCADE,
    CONSTRAINT fk_machine_documents_type    FOREIGN KEY (document_type_id) REFERENCES machine_parameters(parameter_id)
);

-- ------------------------------------------------------------
-- 5) AUDITORÍA DEL MÓDULO
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS machine_audit_logs (
    audit_log_id        BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    machine_id          INTEGER,                 -- máquina afectada (puede ser NULL)
    affected_table_name VARCHAR(50),
    affected_record_id  INTEGER,
    action_type         VARCHAR(50) NOT NULL,    -- MACHINE_CREATED, MACHINE_UPDATED, EVENT_ADDED, ...
    action_description  TEXT,
    old_data            JSONB,
    new_data            JSONB,
    ip_address          VARCHAR(45),
    user_agent          VARCHAR(255),
    executed_by_user_id INTEGER,                 -- usuario de auth-service (no FK)
    executed_at         TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_machine_audit_logs_machine FOREIGN KEY (machine_id) REFERENCES machines(machine_id)
);
