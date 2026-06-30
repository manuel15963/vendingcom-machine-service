# Base de datos — machine-service (VENDINGCOM)

Esquema del microservicio de **máquinas vending**. Comparte la base `vendingcom_db`
(misma Supabase/Postgres que auth, customer y location), separada por **prefijo de
tabla** `machine_*`. **No** crea la base de datos.

La máquina es solo el **activo físico**. El inventario/stock va en
`product-inventory-service` y el dinero en `collection-service`.

## Archivos (ejecutar en orden)

| # | Archivo | Qué hace |
|---|---|---|
| 01 | `01_create_machine_tables.sql` | Tablas + secuencia `seq_machine_code` |
| 02 | `02_create_machine_comments.sql` | Diccionario de datos (COMMENT ON) |
| 03 | `03_create_machine_indexes.sql` | Índices (filtros, joins, búsqueda) |
| 04 | `04_create_machine_triggers.sql` | Triggers `updated_at` y `code` autogenerado |
| 05 | `05_insert_machine_seed_data.sql` | Catálogo base (`machine_parameters`) |
| 99 | `99_select_machine_validation.sql` | Consultas para verificar que todo entró bien |

### Cómo ejecutar
Corre los archivos **en orden (01 → 05)** en el SQL Editor de Supabase (o con `psql`),
y luego `99_...` para verificar. Todos son idempotentes (se pueden re-ejecutar sin romper).
- **Local (psql):** `for f in 0*.sql; do psql -d vendingcom_db -f "$f"; done`

## 5 tablas
- **machine_parameters** — catálogo (MACHINE_STATUS, EVENT_TYPE, DOCUMENT_TYPE).
- **machines** — máquina (code autogenerado `VEND-000001`, qr_code, estado, configuración JSONB, `version` para bloqueo optimista).
- **machine_events** — eventos (instalación, mantenimiento, reparación…).
- **machine_documents** — documentos (archivo en S3/MinIO; aquí solo la URL).
- **machine_audit_logs** — auditoría append-only (old/new JSONB).

## Decisiones de diseño
- **IDs propios INTEGER** (`GENERATED ALWAYS AS IDENTITY`); `customer_id`/`location_id`/`*_user_id` son INTEGER de otros servicios, **sin FK física** (se validan por HTTP).
- Estados y tipos por **tabla catálogo** (`machine_parameters`), no enums nativos — agregar un valor es solo un INSERT.
- `code` autogenerado por secuencia + trigger (`VEND-000001`); `qr_code` por defecto = `code`.
- `version` para bloqueo optimista; auditoría con `old_data`/`new_data` JSONB.
- Mismo estándar que `customer-service` / `auth-service`.
