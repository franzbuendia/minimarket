-- Script de migración: Agregar columnas para soporte de escaneo global y almacenamiento local de fotos
USE `minimarket`;

SET @dbname = DATABASE();
SET @tablename = 'productos';

-- Columna marca
SET @columnname = 'marca';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `productos` ADD COLUMN `marca` VARCHAR(100) NULL COMMENT \'Marca comercial\' AFTER `nombre`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Columna categoria_nombre
SET @columnname = 'categoria_nombre';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `productos` ADD COLUMN `categoria_nombre` VARCHAR(100) NULL COMMENT \'Nombre legible de la categoría\' AFTER `id_categoria`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- Columna imagen_ruta
SET @columnname = 'imagen_ruta';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE `productos` ADD COLUMN `imagen_ruta` VARCHAR(255) NULL COMMENT \'Ruta local de la foto del producto\' AFTER `imagen_url`'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;
