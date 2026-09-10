-- ============================================================
--  seed_datos.sql — Datos iniciales para "La Canasta"
--  Ejecutar DESPUÉS de prueba1.sql (crea el esquema vacío).
-- ============================================================

USE minimarket;

-- Usuarios (contraseñas en texto plano para la fase de pruebas;
-- en producción se almacenarán con BCrypt).
INSERT INTO usuarios (nombre, usuario, contrasena, rol, estado) VALUES
('Administrador', 'admin', 'admin123', 'ADMIN', 1),
('Cajero Principal', 'cajero1', 'cajero123', 'CAJERO', 1);

-- Categorías
INSERT INTO categorias (nombre, descripcion) VALUES
('Bebidas', 'Gaseosas, jugos, aguas'),
('Abarrotes', 'Arroz, fideos, azúcar'),
('Limpieza', 'Detergentes, lejía'),
('Lácteos', 'Leche, yogurt, queso');

-- Productos (precio_normal y precio_descuento)
INSERT INTO productos (codigo_barras, nombre, precio_normal, precio_descuento, stock, estado, id_categoria, usuario_creacion) VALUES
('7891000012345', 'Coca-Cola 1.5L', 4.50, 4.00, 50, 1, 1, 1),
('7891000056789', 'Fideos Don Vittorio 500g', 3.20, 0.00, 40, 1, 2, 1),
('7891000098765', 'Lejía Clorox 1L', 5.00, 0.00, 20, 1, 3, 1),
('7891000032100', 'Leche Gloria 1L', 4.80, 4.50, 30, 1, 4, 1);

-- Configuración de la empresa (singleton)
INSERT INTO empresa_configuracion (id_config, nombre_comercial, ruc, direccion, telefono, mensaje_ticket) VALUES
(1, 'La Canasta', '12345678901', 'Av. Principal 123', '999888777', '¡Gracias por su compra!');
