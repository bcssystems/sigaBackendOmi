-- Módulo Maker para SIGA.
-- No utiliza producto, inventario_sucursal ni movimiento_stock.
-- Ejecutar con respaldo previo de la base de datos. spring.jpa.hibernate.ddl-auto=update
-- también puede crear las tablas nuevas en instalaciones existentes.

CREATE TABLE IF NOT EXISTS maker_material (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    activo BIT NOT NULL,
    UNIQUE KEY uk_maker_material_nombre (nombre)
);

CREATE TABLE IF NOT EXISTS maker_filamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    material_id INT NOT NULL,
    precio_kilo DECIMAL(12,2) NOT NULL,
    stock_kg DECIMAL(12,3) NOT NULL DEFAULT 0,
    marca VARCHAR(100),
    color VARCHAR(80),
    activo BIT NOT NULL,
    KEY idx_maker_filamento_activo_nombre (activo, nombre),
    CONSTRAINT fk_maker_filamento_material
        FOREIGN KEY (material_id) REFERENCES maker_material(id)
);

CREATE TABLE IF NOT EXISTS maker_filamento_movimiento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    filamento_id INT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    cantidad DECIMAL(12,3) NOT NULL,
    stock_anterior DECIMAL(12,3) NOT NULL,
    stock_nuevo DECIMAL(12,3) NOT NULL,
    referencia VARCHAR(80),
    usuario VARCHAR(100),
    observacion TEXT,
    fecha_movimiento DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_maker_movimiento_filamento_fecha (filamento_id, fecha_movimiento),
    KEY idx_maker_movimiento_tipo_fecha (tipo, fecha_movimiento),
    CONSTRAINT fk_maker_movimiento_filamento
        FOREIGN KEY (filamento_id) REFERENCES maker_filamento(id)
);

CREATE TABLE IF NOT EXISTS maker_cotizacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    folio VARCHAR(40) NOT NULL UNIQUE,
    fecha DATETIME NOT NULL,
    persona_id INT,
    proyecto_id INT,
    descripcion VARCHAR(160),
    modo VARCHAR(20) NOT NULL,
    gramos_netos DECIMAL(12,3) NOT NULL,
    gramos_merma DECIMAL(12,3) NOT NULL,
    gramos_totales DECIMAL(12,3) NOT NULL,
    horas_impresion DECIMAL(10,2) NOT NULL,
    piezas_por_cama INT NOT NULL,
    consumo_kw DECIMAL(12,4) NOT NULL,
    costo_kwh DECIMAL(12,4) NOT NULL,
    costo_material DECIMAL(12,2) NOT NULL,
    costo_electricidad DECIMAL(12,2) NOT NULL,
    costo_extras DECIMAL(12,2) NOT NULL,
    costo_cama DECIMAL(12,2) NOT NULL,
    costo_pieza DECIMAL(12,2) NOT NULL,
    tipo_precio VARCHAR(20) NOT NULL,
    margen DECIMAL(6,4) NOT NULL,
    precio_cama DECIMAL(12,2) NOT NULL,
    precio_pieza DECIMAL(12,2) NOT NULL,
    precio_cama_final DECIMAL(12,2),
    precio_pieza_final DECIMAL(12,2),
    KEY idx_maker_cotizacion_fecha (fecha),
    KEY idx_maker_cotizacion_folio (folio),
    CONSTRAINT fk_maker_cotizacion_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id_persona)
);

CREATE TABLE IF NOT EXISTS maker_cotizacion_filamento (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cotizacion_id INT NOT NULL,
    filamento_id INT,
    nombre_filamento VARCHAR(150) NOT NULL,
    material VARCHAR(100),
    marca VARCHAR(100),
    color VARCHAR(80),
    precio_kilo DECIMAL(12,2) NOT NULL,
    gramos DECIMAL(12,3) NOT NULL,
    costo DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_maker_cf_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES maker_cotizacion(id) ON DELETE CASCADE,
    CONSTRAINT fk_maker_cf_filamento
        FOREIGN KEY (filamento_id) REFERENCES maker_filamento(id)
);

CREATE TABLE IF NOT EXISTS maker_cotizacion_extra (
    id INT AUTO_INCREMENT PRIMARY KEY,
    cotizacion_id INT NOT NULL,
    nombre VARCHAR(150) NOT NULL,
    cantidad DECIMAL(10,2) NOT NULL,
    precio_unitario DECIMAL(12,2) NOT NULL,
    importe DECIMAL(12,2) NOT NULL,
    CONSTRAINT fk_maker_ce_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES maker_cotizacion(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS maker_proyecto (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(160) NOT NULL,
    descripcion TEXT,
    estado VARCHAR(20) NOT NULL,
    prioridad VARCHAR(20) NOT NULL,
    color VARCHAR(20) NOT NULL,
    fecha_fin DATE,
    fecha_creacion DATETIME NOT NULL,
    filamento_descontado BIT NOT NULL DEFAULT 0,
    persona_id INT,
    cotizacion_id INT,
    KEY idx_maker_proyecto_estado (estado),
    KEY idx_maker_proyecto_fecha (fecha_fin),
    CONSTRAINT fk_maker_proyecto_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id_persona),
    CONSTRAINT fk_maker_proyecto_cotizacion
        FOREIGN KEY (cotizacion_id) REFERENCES maker_cotizacion(id)
);

CREATE TABLE IF NOT EXISTS maker_proyecto_persona (
    proyecto_id INT NOT NULL,
    persona_id INT NOT NULL,
    PRIMARY KEY (proyecto_id, persona_id),
    CONSTRAINT fk_maker_pp_proyecto
        FOREIGN KEY (proyecto_id) REFERENCES maker_proyecto(id) ON DELETE CASCADE,
    CONSTRAINT fk_maker_pp_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id_persona)
);

-- La relación cotización-proyecto es circular: se crea el segundo vínculo
-- después de ambas tablas. La comprobación permite ejecutar este bloque más
-- de una vez sin fallar por una constraint ya existente.
SET @maker_cotizacion_proyecto_fk_exists := (
    SELECT COUNT(*)
    FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
    WHERE CONSTRAINT_SCHEMA = DATABASE()
      AND TABLE_NAME = 'maker_cotizacion'
      AND COLUMN_NAME = 'proyecto_id'
      AND REFERENCED_TABLE_NAME = 'maker_proyecto'
);
SET @maker_cotizacion_proyecto_fk_sql := IF(
    @maker_cotizacion_proyecto_fk_exists = 0,
    'ALTER TABLE maker_cotizacion ADD CONSTRAINT fk_maker_cotizacion_proyecto FOREIGN KEY (proyecto_id) REFERENCES maker_proyecto(id)',
    'SELECT 1'
);
PREPARE maker_cotizacion_proyecto_fk_stmt FROM @maker_cotizacion_proyecto_fk_sql;
EXECUTE maker_cotizacion_proyecto_fk_stmt;
DEALLOCATE PREPARE maker_cotizacion_proyecto_fk_stmt;

CREATE TABLE IF NOT EXISTS maker_tarea (
    id INT AUTO_INCREMENT PRIMARY KEY,
    nombre VARCHAR(180) NOT NULL,
    descripcion TEXT,
    checklist TEXT,
    estado VARCHAR(20) NOT NULL,
    prioridad VARCHAR(20) NOT NULL,
    color VARCHAR(20) NOT NULL,
    fecha_fin DATE,
    fecha_creacion DATETIME NOT NULL,
    proyecto_id INT,
    KEY idx_maker_tarea_estado (estado),
    KEY idx_maker_tarea_proyecto (proyecto_id),
    CONSTRAINT fk_maker_tarea_proyecto
        FOREIGN KEY (proyecto_id) REFERENCES maker_proyecto(id) ON DELETE SET NULL
);

CREATE TABLE IF NOT EXISTS maker_tarea_persona (
    tarea_id INT NOT NULL,
    persona_id INT NOT NULL,
    PRIMARY KEY (tarea_id, persona_id),
    CONSTRAINT fk_maker_tp_tarea
        FOREIGN KEY (tarea_id) REFERENCES maker_tarea(id) ON DELETE CASCADE,
    CONSTRAINT fk_maker_tp_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id_persona)
);

CREATE TABLE IF NOT EXISTS maker_tarea_mencion (
    tarea_id INT NOT NULL,
    mencionada_id INT NOT NULL,
    PRIMARY KEY (tarea_id, mencionada_id),
    CONSTRAINT fk_maker_tm_tarea
        FOREIGN KEY (tarea_id) REFERENCES maker_tarea(id) ON DELETE CASCADE,
    CONSTRAINT fk_maker_tm_mencionada
        FOREIGN KEY (mencionada_id) REFERENCES maker_tarea(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS maker_notificacion (
    id INT AUTO_INCREMENT PRIMARY KEY,
    persona_id INT NOT NULL,
    titulo VARCHAR(180) NOT NULL,
    mensaje VARCHAR(500) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    referencia_tipo VARCHAR(30),
    referencia_id INT,
    leida BIT NOT NULL DEFAULT 0,
    fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_maker_notificacion_persona (persona_id, leida, fecha),
    CONSTRAINT fk_maker_notificacion_persona
        FOREIGN KEY (persona_id) REFERENCES persona(id_persona) ON DELETE CASCADE
);

-- Configuración Maker reutilizando la tabla general de SIGA.
INSERT IGNORE INTO configuracion (clave, valor, descripcion, actualizada_en) VALUES
('maker.costo_kwh', '3.50', 'Costo de electricidad (MXN/kWh)', NOW()),
('maker.consumo_kw', '0.095', 'Consumo promedio de Bambu Lab A1 durante impresión PLA (95 W)', NOW()),
('maker.merma_monocolor', '0.03', 'Merma proporcional para impresión monocolor', NOW()),
('maker.merma_multicolor', '0.15', 'Merma proporcional para impresión multicolor', NOW()),
('maker.purga_por_cambio', '3.00', 'Gramos de purga por cambio de color', NOW()),
('maker.margen_publico', '0.50', 'Margen para público', NOW()),
('maker.margen_mayoreo', '0.35', 'Margen para mayoreo', NOW()),
('maker.margen_distribuidor', '0.20', 'Margen para distribuidor', NOW());
