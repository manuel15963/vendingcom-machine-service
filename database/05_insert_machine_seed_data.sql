-- ============================================================
-- VENDING.COM S.A.C — Módulo MACHINE
-- Catálogos base (machine_parameters)
-- Idempotente: ON CONFLICT DO NOTHING (se puede correr varias veces).
-- Grupos: MACHINE_STATUS (4) · MACHINE_TYPE (5) · EVENT_TYPE (6) · DOCUMENT_TYPE (5)
-- ============================================================

INSERT INTO machine_parameters (parameter_group, parameter_code, parameter_value, description, sort_order, parameter_status) VALUES
('MACHINE_STATUS','ACTIVE','Activa','Máquina operativa.',1,1),
('MACHINE_STATUS','INACTIVE','Inactiva','Máquina apagada o no operativa.',2,1),
('MACHINE_STATUS','MAINTENANCE','En mantenimiento','En mantenimiento programado.',3,1),
('MACHINE_STATUS','OUT_OF_SERVICE','Fuera de servicio','Fuera de servicio / baja temporal.',4,1),

('MACHINE_TYPE','SNACKS','Snacks','Máquina de snacks / golosinas.',1,1),
('MACHINE_TYPE','COLD_DRINKS','Bebidas frías','Máquina de bebidas frías (refrigerada).',2,1),
('MACHINE_TYPE','COFFEE','Café','Máquina de café / bebidas calientes.',3,1),
('MACHINE_TYPE','COMBO','Combinada','Snacks y bebidas en una sola máquina.',4,1),
('MACHINE_TYPE','OTHER','Otra','Otro tipo de máquina.',5,1),

('EVENT_TYPE','INSTALLATION','Instalación','Instalación de la máquina.',1,1),
('EVENT_TYPE','MAINTENANCE','Mantenimiento','Mantenimiento realizado.',2,1),
('EVENT_TYPE','REPAIR','Reparación','Reparación realizada.',3,1),
('EVENT_TYPE','STATUS_CHANGE','Cambio de estado','Cambio de estado de la máquina.',4,1),
('EVENT_TYPE','CONFIG_CHANGE','Cambio de configuración','Cambio de configuración técnica.',5,1),
('EVENT_TYPE','OTHER','Otro evento','Otro evento.',6,1),

('DOCUMENT_TYPE','MANUAL','Manual de usuario','Manual de usuario de la máquina.',1,1),
('DOCUMENT_TYPE','WARRANTY','Garantía','Documento de garantía.',2,1),
('DOCUMENT_TYPE','TECHNICAL_SHEET','Ficha técnica','Ficha técnica de la máquina.',3,1),
('DOCUMENT_TYPE','PHOTO','Fotografía','Fotografía de la máquina.',4,1),
('DOCUMENT_TYPE','OTHER','Otro documento','Otro documento.',5,1)
ON CONFLICT (parameter_group, parameter_code) DO NOTHING;
