-- =============================================================================
-- SCRIPT DE INICIALIZACIÓN Y RECONSTRUCCIÓN: BASE DE DATOS 'minimarket'
-- Sistema de Minimarket POS "La Canasta"
-- Charset: utf8mb4 | Collation: utf8mb4_general_ci
-- Fecha de generación: 2026-11-14
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `minimarket`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_general_ci;

USE `minimarket`;

-- Desactivar temporalmente restricciones de claves foráneas para carga limpia
SET @OLD_FOREIGN_KEY_CHECKS = @@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS = 0;
SET @OLD_SQL_MODE = @@SQL_MODE, SQL_MODE = 'NO_AUTO_VALUE_ON_ZERO';

-- =============================================================================
-- 1. TABLA: usuarios
-- =============================================================================
DROP TABLE IF EXISTS `usuarios`;
CREATE TABLE `usuarios` (
  `id_usuario` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL COMMENT 'Nombre completo del colaborador',
  `usuario` VARCHAR(50) NOT NULL COMMENT 'Nombre de usuario único para inicio de sesión',
  `contrasena` VARCHAR(255) NOT NULL COMMENT 'Contraseña hasheada (BCrypt) o en texto plano para desarrollo',
  `rol` ENUM('ADMIN','CAJERO') NOT NULL DEFAULT 'CAJERO' COMMENT 'Rol asignado para permisos',
  `estado` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=activo, 0=inactivo',
  `fecha_creacion` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de registro del usuario',
  PRIMARY KEY (`id_usuario`),
  UNIQUE KEY `uq_usuarios_usuario` (`usuario`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Usuarios del sistema POS';

-- =============================================================================
-- 2. TABLA: categorias
-- =============================================================================
DROP TABLE IF EXISTS `categorias`;
CREATE TABLE `categorias` (
  `id_categoria` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(50) NOT NULL COMMENT 'Nombre de la categoría',
  `descripcion` VARCHAR(200) DEFAULT NULL COMMENT 'Descripción opcional',
  PRIMARY KEY (`id_categoria`),
  UNIQUE KEY `uq_categorias_nombre` (`nombre`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Categorías de productos';

-- =============================================================================
-- 3. TABLA: empresa_configuracion
-- =============================================================================
DROP TABLE IF EXISTS `empresa_configuracion`;
CREATE TABLE `empresa_configuracion` (
  `id_config` INT NOT NULL COMMENT 'Identificador único (registro singleton = 1)',
  `nombre_comercial` VARCHAR(100) NOT NULL COMMENT 'Razón comercial del negocio',
  `ruc` VARCHAR(20) NOT NULL COMMENT 'Registro Único de Contribuyentes',
  `direccion` VARCHAR(200) NOT NULL COMMENT 'Dirección física del establecimiento',
  `telefono` VARCHAR(20) NOT NULL COMMENT 'Teléfono de contacto',
  `mensaje_ticket` VARCHAR(255) DEFAULT '¡Gracias por su compra!' COMMENT 'Mensaje al pie del comprobante',
  PRIMARY KEY (`id_config`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Configuración global de la empresa';

-- =============================================================================
-- 4. TABLA: proveedores
-- =============================================================================
DROP TABLE IF EXISTS `proveedores`;
CREATE TABLE `proveedores` (
  `id_proveedor` INT NOT NULL AUTO_INCREMENT,
  `nombre` VARCHAR(100) NOT NULL COMMENT 'Razón social o nombre del proveedor',
  `contacto` VARCHAR(100) DEFAULT NULL COMMENT 'Nombre de la persona de contacto',
  `telefono` VARCHAR(15) DEFAULT NULL COMMENT 'Teléfono del proveedor',
  `direccion` VARCHAR(200) DEFAULT NULL COMMENT 'Dirección del proveedor',
  `estado` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=activo, 0=inactivo',
  `usuario_creacion` INT DEFAULT NULL COMMENT 'Usuario que registró al proveedor',
  `fecha_creacion` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de alta',
  `usuario_modificacion` INT DEFAULT NULL COMMENT 'Último usuario en modificar',
  `fecha_modificacion` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Fecha de última actualización',
  PRIMARY KEY (`id_proveedor`),
  KEY `fk_proveedores_usuario_creacion` (`usuario_creacion`),
  KEY `fk_proveedores_usuario_modificacion` (`usuario_modificacion`),
  CONSTRAINT `fk_proveedores_usuario_creacion` FOREIGN KEY (`usuario_creacion`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL,
  CONSTRAINT `fk_proveedores_usuario_modificacion` FOREIGN KEY (`usuario_modificacion`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Proveedores de mercancía';

-- =============================================================================
-- 5. TABLA: productos
-- =============================================================================
DROP TABLE IF EXISTS `productos`;
CREATE TABLE `productos` (
  `id_producto` INT NOT NULL AUTO_INCREMENT,
  `codigo_barras` VARCHAR(50) DEFAULT NULL COMMENT 'Código de barras para lector o búsqueda rápida',
  `nombre` VARCHAR(100) NOT NULL COMMENT 'Descripción comercial del producto',
  `marca` VARCHAR(100) DEFAULT NULL COMMENT 'Marca comercial del producto',
  `precio_normal` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Precio de venta principal',
  `precio_descuento` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Precio de oferta o secundario',
  `costo` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Costo de adquisición unitario',
  `stock` INT NOT NULL DEFAULT 0 COMMENT 'Existencias físicas actuales',
  `estado` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '1=activo, 0=inactivo',
  `id_categoria` INT DEFAULT NULL COMMENT 'Categoría a la que pertenece',
  `categoria_nombre` VARCHAR(100) DEFAULT NULL COMMENT 'Nombre legible de la categoría',
  `imagen_url` VARCHAR(255) DEFAULT NULL COMMENT 'URL remota de la foto del producto',
  `imagen_ruta` VARCHAR(255) DEFAULT NULL COMMENT 'Ruta local en disco de la foto del producto',
  `usuario_creacion` INT DEFAULT NULL COMMENT 'Usuario que creó el registro',
  `fecha_creacion` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de ingreso al inventario',
  `usuario_modificacion` INT DEFAULT NULL COMMENT 'Último usuario que actualizó',
  `fecha_modificacion` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Última fecha de modificación',
  PRIMARY KEY (`id_producto`),
  UNIQUE KEY `uq_productos_codigo_barras` (`codigo_barras`),
  KEY `idx_productos_categoria` (`id_categoria`),
  KEY `fk_productos_usuario_creacion` (`usuario_creacion`),
  KEY `fk_productos_usuario_modificacion` (`usuario_modificacion`),
  CONSTRAINT `chk_productos_stock` CHECK (`stock` >= 0),
  CONSTRAINT `fk_productos_categoria` FOREIGN KEY (`id_categoria`) REFERENCES `categorias` (`id_categoria`) ON DELETE SET NULL,
  CONSTRAINT `fk_productos_usuario_creacion` FOREIGN KEY (`usuario_creacion`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL,
  CONSTRAINT `fk_productos_usuario_modificacion` FOREIGN KEY (`usuario_modificacion`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Catálogo de productos e inventario';

-- =============================================================================
-- 6. TABLA: caja
-- =============================================================================
DROP TABLE IF EXISTS `caja`;
CREATE TABLE `caja` (
  `id_caja` INT NOT NULL AUTO_INCREMENT,
  `fecha_apertura` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de inicio de turno',
  `fecha_cierre` DATETIME DEFAULT NULL COMMENT 'Fecha y hora de cierre de turno',
  `monto_inicial` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Saldo base en caja al abrir',
  `monto_final` DECIMAL(10,2) DEFAULT NULL COMMENT 'Monto físico contado al cierre',
  `monto_esperado` DECIMAL(10,2) DEFAULT NULL COMMENT 'Monto calculado por el sistema',
  `diferencia` DECIMAL(10,2) DEFAULT NULL COMMENT 'Diferencia (sobrante o faltante)',
  `usuario_apertura` INT NOT NULL COMMENT 'Cajero que abre turno',
  `usuario_cierre` INT DEFAULT NULL COMMENT 'Cajero o supervisor que cierra turno',
  `estado` ENUM('ABIERTO','CERRADO') DEFAULT 'ABIERTO' COMMENT 'Estado del turno',
  PRIMARY KEY (`id_caja`),
  KEY `fk_caja_usuario_apertura` (`usuario_apertura`),
  KEY `fk_caja_usuario_cierre` (`usuario_cierre`),
  CONSTRAINT `fk_caja_usuario_apertura` FOREIGN KEY (`usuario_apertura`) REFERENCES `usuarios` (`id_usuario`) ON DELETE RESTRICT,
  CONSTRAINT `fk_caja_usuario_cierre` FOREIGN KEY (`usuario_cierre`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Control de turnos y arqueos de caja';

-- =============================================================================
-- 7. TABLA: movimientos_caja
-- =============================================================================
DROP TABLE IF EXISTS `movimientos_caja`;
CREATE TABLE `movimientos_caja` (
  `id_movimiento` INT NOT NULL AUTO_INCREMENT,
  `id_caja` INT NOT NULL COMMENT 'Turno de caja asociado',
  `fecha` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora del movimiento',
  `tipo` ENUM('INGRESO','EGRESO') NOT NULL COMMENT 'Tipo de movimiento manual',
  `monto` DECIMAL(10,2) NOT NULL COMMENT 'Monto en efectivo',
  `descripcion` VARCHAR(200) DEFAULT NULL COMMENT 'Motivo del ingreso o egreso',
  `id_usuario_registro` INT DEFAULT NULL COMMENT 'Usuario que registró el movimiento',
  PRIMARY KEY (`id_movimiento`),
  KEY `fk_movimientos_caja_caja` (`id_caja`),
  KEY `fk_movimientos_caja_usuario` (`id_usuario_registro`),
  CONSTRAINT `fk_movimientos_caja_caja` FOREIGN KEY (`id_caja`) REFERENCES `caja` (`id_caja`) ON DELETE CASCADE,
  CONSTRAINT `fk_movimientos_caja_usuario` FOREIGN KEY (`id_usuario_registro`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Entradas y salidas manuales de efectivo en caja';

-- =============================================================================
-- 8. TABLA: ventas
-- =============================================================================
DROP TABLE IF EXISTS `ventas`;
CREATE TABLE `ventas` (
  `id_venta` INT NOT NULL AUTO_INCREMENT,
  `id_cajero` INT NOT NULL COMMENT 'Cajero que realizó la transacción',
  `id_caja` INT DEFAULT NULL COMMENT 'Turno de caja donde se recaudó',
  `fecha` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora de cobro',
  `total` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Importe total cobrado',
  `metodo_pago` ENUM('EFECTIVO','TARJETA','YAPE') NOT NULL DEFAULT 'EFECTIVO' COMMENT 'Medio de pago utilizado',
  `estado_pago` ENUM('COMPLETADA','ANULADA') DEFAULT 'COMPLETADA' COMMENT 'Estado de la transacción',
  PRIMARY KEY (`id_venta`),
  KEY `fk_ventas_cajero` (`id_cajero`),
  KEY `fk_ventas_caja` (`id_caja`),
  KEY `idx_ventas_fecha` (`fecha`),
  CONSTRAINT `fk_ventas_cajero` FOREIGN KEY (`id_cajero`) REFERENCES `usuarios` (`id_usuario`) ON DELETE RESTRICT,
  CONSTRAINT `fk_ventas_caja` FOREIGN KEY (`id_caja`) REFERENCES `caja` (`id_caja`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Encabezado de transacciones de venta';

-- =============================================================================
-- 9. TABLA: detalle_ventas
-- =============================================================================
DROP TABLE IF EXISTS `detalle_ventas`;
CREATE TABLE `detalle_ventas` (
  `id_detalle_venta` INT NOT NULL AUTO_INCREMENT,
  `id_venta` INT NOT NULL COMMENT 'Venta a la que pertenece el ítem',
  `id_producto` INT NOT NULL COMMENT 'Producto adquirido',
  `cantidad` INT NOT NULL COMMENT 'Unidades vendidas',
  `precio_unitario` DECIMAL(10,2) NOT NULL COMMENT 'Precio unitario al momento de la venta',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT 'Importe parcial (cantidad * precio_unitario)',
  PRIMARY KEY (`id_detalle_venta`),
  KEY `fk_detalle_ventas_venta` (`id_venta`),
  KEY `fk_detalle_ventas_producto` (`id_producto`),
  CONSTRAINT `chk_detalle_ventas_cantidad` CHECK (`cantidad` > 0),
  CONSTRAINT `fk_detalle_ventas_venta` FOREIGN KEY (`id_venta`) REFERENCES `ventas` (`id_venta`) ON DELETE CASCADE,
  CONSTRAINT `fk_detalle_ventas_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Líneas de detalle de cada venta';

-- =============================================================================
-- 10. TABLA: compras
-- =============================================================================
DROP TABLE IF EXISTS `compras`;
CREATE TABLE `compras` (
  `id_compra` INT NOT NULL AUTO_INCREMENT,
  `id_proveedor` INT NOT NULL COMMENT 'Proveedor que emitió la compra',
  `fecha` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha de registro de compra',
  `total` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Importe total de adquisición',
  `id_usuario_registro` INT DEFAULT NULL COMMENT 'Usuario que ingresó la compra',
  PRIMARY KEY (`id_compra`),
  KEY `fk_compras_proveedor` (`id_proveedor`),
  KEY `fk_compras_usuario` (`id_usuario_registro`),
  CONSTRAINT `fk_compras_proveedor` FOREIGN KEY (`id_proveedor`) REFERENCES `proveedores` (`id_proveedor`) ON DELETE RESTRICT,
  CONSTRAINT `fk_compras_usuario` FOREIGN KEY (`id_usuario_registro`) REFERENCES `usuarios` (`id_usuario`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Compras de mercadería a proveedores';

-- =============================================================================
-- 11. TABLA: detalle_compras
-- =============================================================================
DROP TABLE IF EXISTS `detalle_compras`;
CREATE TABLE `detalle_compras` (
  `id_detalle_compra` INT NOT NULL AUTO_INCREMENT,
  `id_compra` INT NOT NULL COMMENT 'Compra asociada',
  `id_producto` INT NOT NULL COMMENT 'Producto recibido',
  `cantidad` INT NOT NULL COMMENT 'Cantidad adquirida',
  `costo_unitario` DECIMAL(10,2) NOT NULL COMMENT 'Costo unitario de compra',
  `subtotal` DECIMAL(10,2) NOT NULL COMMENT 'Subtotal (cantidad * costo_unitario)',
  PRIMARY KEY (`id_detalle_compra`),
  KEY `fk_detalle_compras_compra` (`id_compra`),
  KEY `fk_detalle_compras_producto` (`id_producto`),
  CONSTRAINT `chk_detalle_compras_cantidad` CHECK (`cantidad` > 0),
  CONSTRAINT `fk_detalle_compras_compra` FOREIGN KEY (`id_compra`) REFERENCES `compras` (`id_compra`) ON DELETE CASCADE,
  CONSTRAINT `fk_detalle_compras_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Detalle de productos ingresados en cada compra';

-- =============================================================================
-- 12. TABLA: kardex
-- =============================================================================
DROP TABLE IF EXISTS `kardex`;
CREATE TABLE `kardex` (
  `id_kardex` INT NOT NULL AUTO_INCREMENT,
  `id_producto` INT NOT NULL COMMENT 'Producto en movimiento',
  `id_usuario_movimiento` INT NOT NULL COMMENT 'Usuario responsable del movimiento',
  `fecha_movimiento` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Fecha y hora del movimiento',
  `tipo_movimiento` ENUM('ENTRADA','SALIDA','AJUSTE') NOT NULL COMMENT 'Tipo de flujo de almacén',
  `referencia` VARCHAR(100) DEFAULT NULL COMMENT 'Documento o motivo de referencia (ej. Venta #1)',
  `cantidad` INT NOT NULL COMMENT 'Cantidad involucrada',
  `costo_unitario` DECIMAL(10,2) NOT NULL DEFAULT 0.00 COMMENT 'Costo unitario en ese momento',
  `stock_anterior` INT NOT NULL COMMENT 'Existencias previas al movimiento',
  `stock_resultante` INT NOT NULL COMMENT 'Existencias finales tras el movimiento',
  PRIMARY KEY (`id_kardex`),
  KEY `fk_kardex_producto` (`id_producto`),
  KEY `fk_kardex_usuario` (`id_usuario_movimiento`),
  KEY `idx_kardex_fecha` (`fecha_movimiento`),
  CONSTRAINT `fk_kardex_producto` FOREIGN KEY (`id_producto`) REFERENCES `productos` (`id_producto`) ON DELETE RESTRICT,
  CONSTRAINT `fk_kardex_usuario` FOREIGN KEY (`id_usuario_movimiento`) REFERENCES `usuarios` (`id_usuario`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Libro kardex para auditoría de inventario físico';

-- =============================================================================
-- 13. TABLA: sync_conflictos (Auditoría de sincronización)
-- =============================================================================
DROP TABLE IF EXISTS `sync_conflictos`;
CREATE TABLE `sync_conflictos` (
  `id_conflicto` INT NOT NULL AUTO_INCREMENT,
  `codigo_barras` VARCHAR(50) DEFAULT NULL COMMENT 'Código de barras del producto en conflicto',
  `nombre_producto` VARCHAR(100) DEFAULT NULL COMMENT 'Nombre del producto',
  `campo_conflicto` VARCHAR(50) DEFAULT NULL COMMENT 'Columna donde existió discrepancia',
  `valor_origen` TEXT DEFAULT NULL COMMENT 'Valor presente en la fuente original',
  `valor_destino` TEXT DEFAULT NULL COMMENT 'Valor presente en el destino o versión alterna',
  `criterio_resolucion` VARCHAR(255) DEFAULT NULL COMMENT 'Regla aplicada (ej. mayor stock, más reciente)',
  `fecha_resolucion` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'Momento en que se resolvió el conflicto',
  PRIMARY KEY (`id_conflicto`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Bitácora de resolución de conflictos de sincronización';

-- Reactivar verificación de claves foráneas
SET FOREIGN_KEY_CHECKS = @OLD_FOREIGN_KEY_CHECKS;
SET SQL_MODE = @OLD_SQL_MODE;

-- =============================================================================
-- CARGA DE DATOS INICIALES (SEED DATA)
-- =============================================================================

-- Usuarios base del sistema
INSERT INTO `usuarios` (`id_usuario`, `nombre`, `usuario`, `contrasena`, `rol`, `estado`, `fecha_creacion`) VALUES
(1, 'Administrador', 'admin', 'admin123', 'ADMIN', 1, '2026-09-01 10:00:00'),
(2, 'Cajero Principal', 'cajero1', 'cajero123', 'CAJERO', 1, '2026-09-01 10:00:00');

-- Categorías base
INSERT INTO `categorias` (`id_categoria`, `nombre`, `descripcion`) VALUES
(1, 'Bebidas', 'Gaseosas, jugos, aguas'),
(2, 'Abarrotes', 'Arroz, fideos, azúcar'),
(3, 'Limpieza', 'Detergentes, lejía'),
(4, 'Lácteos', 'Leche, yogurt, queso'),
(5, 'Importados', 'Productos importados e internacionales'),
(6, 'Varios', 'Artículos diversos y miscelánea');

-- Configuración corporativa única
INSERT INTO `empresa_configuracion` (`id_config`, `nombre_comercial`, `ruc`, `direccion`, `telefono`, `mensaje_ticket`) VALUES
(1, 'La Canasta', '12345678901', 'Av. Principal 123', '999888777', '¡Gracias por su compra!');

-- Proveedores iniciales
INSERT INTO `proveedores` (`id_proveedor`, `nombre`, `contacto`, `telefono`, `direccion`, `estado`, `usuario_creacion`, `fecha_creacion`) VALUES
(1, 'Distribuidora Lima S.A.C.', 'Carlos Ramírez', '987654321', 'Av. Los Próceres 456', 1, 1, '2026-09-01 10:00:00'),
(2, 'Comercializadora del Sur', 'Ana Gómez', '976543210', 'Jr. Junín 789', 1, 1, '2026-09-01 10:00:00');

-- =============================================================================
-- IMPORTACIÓN DE LOS 90 PRODUCTOS DESDE minimarket_productos.sql
-- Mapeo a la estructura canónica con 'costo' = 0.00 e 'imagen_url' = NULL
-- =============================================================================

INSERT INTO `productos` (
  `id_producto`, `codigo_barras`, `nombre`, `precio_normal`, `precio_descuento`,
  `costo`, `stock`, `estado`, `id_categoria`, `imagen_url`,
  `usuario_creacion`, `fecha_creacion`, `usuario_modificacion`, `fecha_modificacion`
) VALUES
(1, '7891000012345', 'Coca-Cola 1.5L', 4.50, 4.00, 0.00, 48, 1, 1, NULL, 1, '2026-09-16 21:03:09', NULL, '2026-10-15 15:47:07'),
(2, '7891000056789', 'Fideos Don Vittorio 500g', 3.20, 0.00, 0.00, 37, 1, 2, NULL, 1, '2026-09-16 21:03:09', NULL, '2026-09-16 21:06:50'),
(3, '7891000098765', 'Lejía Clorox 1L', 5.00, 0.00, 0.00, 20, 0, 3, NULL, 1, '2026-09-16 21:03:09', NULL, '2026-10-26 00:25:58'),
(4, '7891000032100', 'Leche Gloria 1L', 4.80, 4.50, 0.00, 29, 0, 4, NULL, 1, '2026-09-16 21:03:09', NULL, '2026-10-26 00:25:54'),
(5, '7751271039260', 'BONLE CRECIMIENTO LATA 390 GR', 3.00, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:04:07', NULL, '2026-10-26 00:25:39'),
(6, '7751271034227', 'BONLE CREMOSA LATA 390 GR', 3.50, 0.00, 0.00, 5, 1, 2, NULL, 1, '2026-10-22 21:04:47', NULL, '2026-10-22 16:04:47'),
(7, '7751271036795', 'BONLE PROTECCION LATA 390 GR', 2.50, 0.00, 0.00, 15, 1, 2, NULL, 1, '2026-10-22 21:05:19', NULL, '2026-10-22 16:05:19'),
(8, '7751271036801', 'BONLE PROTECCION LATA 155 GR', 1.20, 0.00, 0.00, 11, 1, 2, NULL, 1, '2026-10-22 21:06:38', NULL, '2026-10-22 16:43:26'),
(9, '7751271032667', 'GLORIA AZUL LATA 390 GR', 4.00, 0.00, 0.00, 5, 1, 2, NULL, 1, '2026-10-22 21:07:14', NULL, '2026-10-22 16:07:13'),
(10, '7751271032681', 'GLORIA LIGTH LATA 170GR', 2.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:07:59', NULL, '2026-10-22 16:07:58'),
(11, '7751271034135', 'GLORIA LIGTH LATA 390 GR', 4.50, 0.00, 0.00, 6, 1, 2, NULL, 1, '2026-10-22 21:08:30', NULL, '2026-10-22 16:08:30'),
(12, '7751271033640', 'GLORIA ZERO LACTOSA LATA 170 GR', 2.50, 0.00, 0.00, 9, 1, 2, NULL, 1, '2026-10-22 21:09:11', NULL, '2026-10-22 16:09:11'),
(13, '7751271034180', 'GLORIA ZERO LACTOSA LATA 390 GR', 4.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:09:41', NULL, '2026-10-22 16:09:41'),
(14, '7751271034302', 'PURA VIDA LATA 390 GR', 3.50, 0.00, 0.00, 8, 1, 2, NULL, 1, '2026-10-22 21:10:20', NULL, '2026-10-22 16:10:19'),
(15, '7751271034005', 'PURA VIDA LATA 155 GR', 1.70, 0.00, 0.00, 5, 1, 2, NULL, 1, '2026-10-22 21:10:48', NULL, '2026-10-22 16:10:47'),
(16, '7750243066495', 'MIRASOL ACEITE SOYA BOTELLA PLAST 900 ML', 7.50, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:11:57', NULL, '2026-10-22 16:11:57'),
(17, '7750243068000', 'PRIMOR CLASICO VEGETAL BOTELLA PLAST 900 ML', 10.00, 0.00, 0.00, 5, 1, 2, NULL, 1, '2026-10-22 21:12:32', NULL, '2026-10-22 16:12:32'),
(18, '7750243067997', 'PRIMOR PREMIUM VEGETAL BOT PLAS 900 ML', 11.00, 0.00, 0.00, 5, 1, 2, NULL, 1, '2026-10-22 21:13:00', NULL, '2026-10-22 16:12:59'),
(19, '7750463004727', 'ALTOMAYO ARMONICO SOBRE 7 GR', 1.20, 0.00, 0.00, 16, 1, 2, NULL, 1, '2026-10-22 21:13:41', NULL, '2026-10-22 16:13:41'),
(20, '7750463004789', 'ALTOMAYO CLASICO SOBRE 7 GR', 1.00, 0.00, 0.00, 19, 1, 2, NULL, 1, '2026-10-22 21:14:09', NULL, '2026-10-22 16:14:08'),
(21, '7750463004161', 'ALTOMAYO GOURMET SOBRE 7 GR', 1.50, 0.00, 0.00, 80, 1, 2, NULL, 1, '2026-10-22 21:14:42', NULL, '2026-10-22 16:14:41'),
(22, '7750463005243', 'ALTOMAYO GOURMET SOBRE 14 GR', 2.50, 0.00, 0.00, 25, 1, 2, NULL, 1, '2026-10-22 21:15:08', NULL, '2026-10-22 16:15:08'),
(23, '7754308003142', 'CAFETAL PURO INTENSO SOBRE 7 GR', 1.30, 0.00, 0.00, 20, 1, 2, NULL, 1, '2026-10-22 21:15:45', NULL, '2026-10-22 16:15:44'),
(24, '7754308003715', 'CAFETAL GOURMET SOBRE 14 GR', 2.20, 0.00, 0.00, 15, 1, 2, NULL, 1, '2026-10-22 21:16:23', NULL, '2026-10-22 16:16:22'),
(25, '8445291013964', 'NESCAFE TRADICION PURO SOBRE 7 GR', 1.50, 0.00, 0.00, 24, 1, 2, NULL, 1, '2026-10-22 21:17:01', NULL, '2026-10-22 16:17:00'),
(26, '8445292349529', 'KIRMA TORRADO SOBRE 7 GR', 1.20, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:17:40', NULL, '2026-10-22 16:17:39'),
(27, '8445291014008', 'NESCAFE TRADICION PURO SOBRE 15 GR', 2.50, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:18:12', NULL, '2026-10-22 16:18:11'),
(28, '7750885020329', 'FANNY FILETE ATUN AC VEGET LATA 140 GR', 6.00, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:19:05', NULL, '2026-10-22 16:19:04'),
(29, '7750885023726', 'FANNY GRATED ATUN AC VEG LATA 140 GR', 4.50, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:19:34', NULL, '2026-10-22 16:19:34'),
(30, '7751158011969', 'FLORIDA DILETE ATUN AC VEGET LATA 140 GR', 5.50, 0.00, 0.00, 20, 1, 2, NULL, 1, '2026-10-22 21:20:14', NULL, '2026-10-22 16:20:13'),
(31, '7751271033411', 'GLORIA FILETE ATUN ACT VEG LATA 140 GR', 6.00, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:20:48', NULL, '2026-10-22 16:20:47'),
(32, '7750243072311', 'PRIMOR FILETE ATUN AC VEGET LATA 140 GR', 5.50, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:21:15', NULL, '2026-10-22 16:21:15'),
(33, '7750243072304', 'PRIMOR TROZOS ATUN AC VEG LATA 140 GR', 5.50, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:21:54', NULL, '2026-10-22 16:21:53'),
(34, '7750243081566', 'BOLIVAR BEBE ENVUELTO 190 GR', 3.20, 0.00, 0.00, 10, 1, 3, NULL, 1, '2026-10-22 21:22:43', NULL, '2026-10-22 16:22:42'),
(35, '7750243079990', 'BOLIVAR CUIDADO  TOTAL ENV 190 GR', 3.00, 0.00, 0.00, 12, 1, 3, NULL, 1, '2026-10-22 21:23:12', NULL, '2026-10-22 21:24:45'),
(36, '7750243081573', 'MARSELLA LAVANDA Y ROSAS ENV 175 GR', 2.50, 0.00, 0.00, 8, 1, 3, NULL, 1, '2026-10-22 21:23:45', NULL, '2026-10-22 21:24:33'),
(37, '7750243080446', 'TROME ENCANTO FLORAL ENV 190 GR', 1.80, 0.00, 0.00, 10, 1, 3, NULL, 1, '2026-10-22 21:24:14', NULL, '2026-10-22 16:24:13'),
(38, '7500435255202', 'ARIEL PRO CUIDADO BOLSA 400 GR', 6.00, 0.00, 0.00, 10, 1, 3, NULL, 1, '2026-10-22 21:25:20', NULL, '2026-10-22 16:25:19'),
(39, '7500435255332', 'ARIEL PRO CUIDADO BOLSA 720 GR', 10.00, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:25:46', NULL, '2026-10-22 16:25:45'),
(40, '7500435255295', 'ARIEL REVITACOLOR BOLSA 720 GR', 10.00, 0.00, 0.00, 2, 1, 3, NULL, 1, '2026-10-22 21:26:09', NULL, '2026-10-22 21:26:20'),
(41, '7750243079068', 'BOLIVAR CUIDADO TOTAL BOLSA 430 GR', 7.00, 0.00, 0.00, 1, 1, 3, NULL, 1, '2026-10-22 21:26:55', NULL, '2026-10-22 16:26:55'),
(42, '7750243079075', 'BOLIVAR CUIDADO TOTAL BOLSA 320 GR', 6.00, 0.00, 0.00, 10, 1, 3, NULL, 1, '2026-10-22 21:27:27', NULL, '2026-10-22 16:27:26'),
(43, '7750243079051', 'BOLIVAR CUIDADO TOTAL BOLSA 730 GR', 9.00, 0.00, 0.00, 2, 1, 3, NULL, 1, '2026-10-22 21:28:01', NULL, '2026-10-22 16:28:01'),
(44, '7750243079167', 'MARSELLA LAVANDA Y ROSAS BOLSA 430 GR', 5.50, 0.00, 0.00, 6, 1, 2, NULL, 1, '2026-10-22 21:28:40', NULL, '2026-10-22 16:28:39'),
(45, '7750243079433', 'MARSELLA LAVANDA Y ROSAS BOLSA 320 GR', 3.70, 0.00, 0.00, 5, 1, 3, NULL, 1, '2026-10-22 21:29:09', NULL, '2026-10-22 16:29:08'),
(46, '7750243079143', 'MARSELLA LAVANDA Y ROSAS BOLSA 730 GR', 7.00, 0.00, 0.00, 8, 1, 3, NULL, 1, '2026-10-22 21:29:31', NULL, '2026-10-22 16:29:30'),
(47, '7750243080644', 'OPAL ULTRA FLORAL BOLSA 320 GR', 5.00, 0.00, 0.00, 5, 1, 3, NULL, 1, '2026-10-22 21:30:02', NULL, '2026-10-22 16:30:02'),
(48, '7750243079693', 'PATITO LIMON BOLSA 140 GR', 1.20, 0.00, 0.00, 60, 1, 3, NULL, 1, '2026-10-22 21:30:39', NULL, '2026-10-22 16:30:39'),
(49, '7750243079761', 'PATITO LIMON BOLSA 1000 GR', 5.50, 0.00, 0.00, 12, 1, 3, NULL, 1, '2026-10-22 21:32:32', NULL, '2026-10-22 16:32:31'),
(50, '7750243082136', 'TROME ENCANTO FLORAL BOLSA 140 GR', 1.00, 0.00, 0.00, 25, 1, 3, NULL, 1, '2026-10-22 21:40:45', NULL, '2026-10-26 00:13:07'),
(51, '7750243079686', 'TROME ENCANTO FLORAL BOLSA 1000 GR', 5.00, 0.00, 0.00, 10, 1, 3, NULL, 1, '2026-10-22 21:42:58', NULL, '2026-10-26 00:13:02'),
(52, '7751271031745', 'GLORIA BATTIMIX BOLITAS CHOC 146 GR', 3.50, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:45:08', NULL, '2026-10-22 16:45:08'),
(53, '7751271036436', 'GLORIA YOGURT DURAZNO BOT PLAST 946 GR', 6.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:46:07', NULL, '2026-10-22 16:46:07'),
(54, '7751271036450', 'GLORIA YOGURT LUCUMA BOT PLAST 946 GR', 6.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:46:40', NULL, '2026-10-22 16:46:40'),
(55, '7751271030328', 'GLORIA YOGURT BOT PLAST 180 GR', 2.00, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:47:06', NULL, '2026-10-22 16:47:06'),
(56, '7751271030359', 'GLORIA YOGURT VAINILLA BOT PLAST 180 GR', 2.00, 0.00, 0.00, 10, 1, 2, NULL, 1, '2026-10-22 21:47:47', NULL, '2026-10-22 16:47:46'),
(57, '7751271036443', 'GLORIA YOGURT VAINILLA BOT PLAST 946 GR', 6.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:48:12', NULL, '2026-10-22 16:48:12'),
(58, '7751271036429', 'GLORIA YOGURT FRESA BOT PLAST 946 GR', 6.50, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:48:44', NULL, '2026-10-22 16:48:43'),
(59, '7751271029186', 'GLORIA YOGURT FRESA BOT PLAST 180 GR', 2.00, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-22 21:49:10', NULL, '2026-10-22 16:49:09'),
(60, '7750151010177', 'LAIVE YOGURT FRESA BOT PLAST 930 GR', 6.50, 0.00, 0.00, 2, 1, 2, NULL, 1, '2026-10-22 21:49:53', NULL, '2026-10-22 16:49:52'),
(61, '7752748012755', 'WINTERS COCOA SOBRE  10 GR', 0.00, 0.00, 0.00, 0, 1, 2, NULL, 1, '2026-10-22 21:53:07', NULL, '2026-10-22 16:53:06'),
(62, '7750670014250', 'BIG COLA BOT PLAST DES 400 ML', 1.00, 0.00, 0.00, 15, 1, 1, NULL, 1, '2026-10-26 00:11:25', NULL, '2026-10-26 00:12:48'),
(63, '7750670016193', 'BIG COLA BOT PLAS DESC 3030 ML', 6.50, 0.00, 0.00, 12, 1, 1, NULL, 1, '2026-10-26 00:11:51', NULL, '2026-10-26 00:12:44'),
(64, '7750182006088', 'INCA KOLA BOT PLAST DESC 600 ML', 3.50, 0.00, 0.00, 5, 1, 1, NULL, 1, '2026-10-26 00:12:36', NULL, '2026-10-26 00:12:56'),
(65, '7750182003834', 'INCA KOLA ZERO BOT PLAS DESC 300 ML', 1.80, 0.00, 0.00, 6, 1, 1, NULL, 1, '2026-10-26 00:13:37', NULL, '2026-10-25 19:13:36'),
(66, '7753749009331', 'BACKUS GUARANITA BOT PLAS DESC 300 ML', 1.00, 0.00, 0.00, 5, 1, 1, NULL, 1, '2026-10-26 00:14:34', NULL, '2026-10-25 19:14:34'),
(67, '7751731006337', 'SABOR DE ORO BOT PLAS DES 350 ML', 1.00, 0.00, 0.00, 9, 1, 1, NULL, 1, '2026-10-26 00:15:15', NULL, '2026-10-25 19:15:14'),
(68, '7750670013468', 'SABOR DE ORO BOT PLS DES 400 ML', 1.00, 0.00, 0.00, 12, 1, 1, NULL, 1, '2026-10-26 00:15:43', NULL, '2026-10-25 19:15:42'),
(69, '7750670016148', 'SABOR DE ORO BOT PLA DES 3030 ML', 6.50, 0.00, 0.00, 2, 1, 1, NULL, 1, '2026-10-26 00:16:11', NULL, '2026-10-25 19:16:10'),
(70, '7750670020411', 'CIELO GASIFICADO BOT PLAS 625 ML', 1.20, 0.00, 0.00, 3, 1, 1, NULL, 1, '2026-10-26 00:17:05', NULL, '2026-10-25 19:17:05'),
(71, '7750670009041', 'CIELO SIN GAS BOT PLAST 1000 ML', 1.00, 0.00, 0.00, 30, 1, 2, NULL, 1, '2026-10-26 00:18:07', NULL, '2026-10-25 19:18:06'),
(72, '7750182001199', 'SAN LUIS GASIFICADA BOT PLAS 625 ML', 1.80, 0.00, 0.00, 6, 1, 1, NULL, 1, '2026-10-26 00:18:41', NULL, '2026-10-25 19:18:41'),
(73, '7750182001229', 'SAN LUIS SIN GAS BOT PLAST 1000 ML', 2.50, 0.00, 0.00, 2, 1, 1, NULL, 1, '2026-10-26 00:19:15', NULL, '2026-10-25 19:19:15'),
(74, '7750182007269', 'SAN LUIS SIN GAS BOT PLAS 750 ML', 1.70, 0.00, 0.00, 6, 1, 1, NULL, 1, '2026-10-26 00:19:52', NULL, '2026-10-25 19:19:52'),
(75, '7753749006156', 'MIKES PASSION FRUIT LEMONADE LATA 355 ML', 6.00, 0.00, 0.00, 5, 1, 1, NULL, 1, '2026-10-26 00:20:34', NULL, '2026-10-25 19:20:33'),
(76, '7750243034562', 'SODA V CRACKERS PAQUETE 35 GR', 0.80, 0.00, 0.00, 15, 1, 4, NULL, 1, '2026-10-26 00:21:32', NULL, '2026-10-25 19:21:31'),
(77, '7758071002074', 'ANGEL ZUCK HOJUELAS BOLSA 130 GR', 3.50, 0.00, 0.00, 3, 1, 2, NULL, 1, '2026-10-26 00:22:03', NULL, '2026-10-25 19:22:03'),
(78, '7759826002899', 'ANGEL COPIX COPOS BOLSA 120 GR', 3.50, 0.00, 0.00, 8, 1, 2, NULL, 1, '2026-10-26 00:22:43', NULL, '2026-10-25 19:22:43'),
(79, '7758071002081', 'ANGEL CHOCK BOLITAS.CHOC. BOLSA 130 GR', 3.50, 0.00, 0.00, 12, 1, 2, NULL, 1, '2026-10-26 00:23:15', NULL, '2026-10-25 19:23:15'),
(80, '7500435172547', 'AYUDIN LIMON Y SAB POTE 285 GR', 5.00, 0.00, 0.00, 2, 1, 3, NULL, 1, '2026-10-26 00:23:58', NULL, '2026-10-25 19:23:58'),
(81, '7753749003186', 'BACKUS GUARANA BOT.PLAS.DES. 450 ML', 2.00, 0.00, 0.00, 5, 1, 1, NULL, 1, '2026-10-26 00:24:34', NULL, '2026-10-25 19:24:34'),
(82, '7750670015042', 'BIG COLA BOT.PLAS.DES. 1035 ML', 2.50, 0.00, 0.00, 6, 1, 1, NULL, 1, '2026-10-26 00:25:13', NULL, '2026-10-25 19:25:13'),
(83, '7750727865477', 'BIMBO PANETON FRUT.PASA. BOL 900 GR', 18.00, 0.00, 0.00, 3, 1, 4, NULL, 1, '2026-10-26 00:26:48', NULL, '2026-10-25 19:26:47'),
(84, '7750727014240', 'BIMBO BIMBOLETES MARMOLEADO 2U 70 GR', 2.50, 0.00, 0.00, 10, 1, 4, NULL, 1, '2026-10-26 00:27:18', NULL, '2026-10-25 19:27:18'),
(85, '7750727014233', 'BIMBO BIMBOLETES VAINILLA 2U 70  GR', 2.50, 0.00, 0.00, 5, 1, 4, NULL, 1, '2026-10-26 00:28:07', NULL, '2026-10-25 19:28:07'),
(86, '7750670017107', 'BIO ALOE VERA BOT.PLS. 500 ML', 3.00, 0.00, 0.00, 5, 1, 1, NULL, 1, '2026-10-26 00:28:53', NULL, '2026-10-25 19:28:53'),
(87, '7891150044500', 'CAMAY BOTANICALS.CLASICO . ENV 125 GR', 3.00, 0.00, 0.00, 5, 1, 3, NULL, 1, '2026-10-26 00:29:39', NULL, '2026-10-25 19:29:39'),
(88, '7751738000062', 'CARTAVIO BLACK RON PETACA 125 ML', 8.00, 0.00, 0.00, 6, 1, 1, NULL, 1, '2026-10-26 00:30:07', NULL, '2026-10-25 19:30:07'),
(89, '7751738053976', 'CARTAVIO DORADO RON PETACA 125 ML', 8.00, 0.00, 0.00, 4, 1, 1, NULL, 1, '2026-10-26 00:30:38', NULL, '2026-10-25 19:30:38'),
(90, '7750885018821', 'CEREAL BAR GOLDEN PAQ 21 GR', 1.00, 0.00, 0.00, 5, 1, 4, NULL, 1, '2026-10-26 00:31:09', NULL, '2026-10-25 19:31:09');
