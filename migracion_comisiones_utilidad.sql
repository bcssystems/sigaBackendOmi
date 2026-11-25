-- Migracion de comisiones por utilidad y eliminacion del sueldo base de persona.
-- Respaldar la base antes de ejecutar.
-- Esta migracion NO elimina nomina.sueldo_base: ese valor es el historico de cada nomina.

-- Conserva el costo unitario al momento de la venta. Las ventas antiguas se
-- completan abajo con el costo actual del producto cuando existe.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'venta_detalle'
              AND column_name = 'costo_unitario'
        ),
        'SELECT 1',
        'ALTER TABLE venta_detalle ADD COLUMN costo_unitario DECIMAL(19,2) NULL AFTER subtotal'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE venta_detalle vd
LEFT JOIN producto p ON p.id_producto = vd.id_producto
SET vd.costo_unitario = COALESCE(p.costo_promedio, 0)
WHERE vd.costo_unitario IS NULL;

-- Elimina unicamente el sueldo del perfil de persona.
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = 'persona'
              AND column_name = 'sueldo_base'
        ),
        'ALTER TABLE persona DROP COLUMN sueldo_base',
        'SELECT 1'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Verificacion: debe mostrar costo_unitario en venta_detalle y no mostrar
-- sueldo_base en persona. nomina.sueldo_base debe seguir existiendo.
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND ((table_name = 'persona' AND column_name = 'sueldo_base')
    OR (table_name = 'nomina' AND column_name = 'sueldo_base')
    OR (table_name = 'venta_detalle' AND column_name = 'costo_unitario'));
