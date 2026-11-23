package com.lacanasta.service;

import com.lacanasta.dao.ProductoDAO;
import com.lacanasta.dao.jdbc.ProductoDAOJdbc;
import com.lacanasta.model.Producto;
import com.lacanasta.util.ConnectionProvider;
import com.lacanasta.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lógica de negocio para la gestión de productos.
 */
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    private final ProductoDAO productoDAO;
    private final ConnectionProvider connectionProvider;

    public ProductoService() {
        this(new ProductoDAOJdbc(), new DatabaseConnection());
    }

    public ProductoService(ProductoDAO productoDAO, ConnectionProvider connectionProvider) {
        this.productoDAO = productoDAO;
        this.connectionProvider = connectionProvider;
    }

    /**
     * Lista todos los productos.
     */
    public List<Producto> findAll() {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findAll(conn);
        } catch (SQLException e) {
            throw new ServiceException("Error al listar productos", e);
        }
    }

    /**
     * Busca un producto por su identificador.
     */
    public Producto findById(int id) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findById(conn, id);
        } catch (SQLException e) {
            throw new ServiceException("Error al buscar el producto", e);
        }
    }

    /**
     * Busca un producto por su código de barras.
     */
    public Producto findByCodigoBarras(String codigoBarras) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findByCodigoBarras(conn, codigoBarras);
        } catch (SQLException e) {
            throw new ServiceException("Error al buscar el producto por código de barras", e);
        }
    }

    /**
     * Busca productos cuyo nombre coincida parcialmente con el texto dado.
     */
    public List<Producto> search(String nombre) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findByNombre(conn, nombre);
        } catch (SQLException e) {
            throw new ServiceException("Error al buscar productos", e);
        }
    }

    /**
     * Lista los productos de una categoría.
     */
    public List<Producto> findByCategoria(int idCategoria) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findByCategoria(conn, idCategoria);
        } catch (SQLException e) {
            throw new ServiceException("Error al buscar productos por categoría", e);
        }
    }

    /**
     * Lista los productos con stock bajo (igual o menor al límite).
     */
    public List<Producto> findByStockBajo(int limite) {
        try (Connection conn = connectionProvider.getConnection()) {
            return productoDAO.findByStockBajo(conn, limite);
        } catch (SQLException e) {
            throw new ServiceException("Error al buscar productos con stock bajo", e);
        }
    }

    /**
     * Guarda un producto (inserta o actualiza) estableciendo las fechas de auditoría y validando integridad.
     */
    public void save(Producto producto) {
        if (producto == null) {
            throw new IllegalArgumentException("El producto a guardar no puede ser nulo.");
        }

        log.info("[GLOBAL-SCAN] [GUARDAR] Solicitud de guardado: id={}, codigo={}, nombre='{}', marca='{}', precioNormal={}, costo={}, stock={}, idCat={}, usuario={}",
                producto.getIdProducto(), producto.getCodigoBarras(), producto.getNombre(), producto.getMarca(),
                producto.getPrecioNormal(), producto.getCosto(), producto.getStock(),
                producto.getIdCategoria(), producto.getUsuarioCreacion());

        try (Connection conn = connectionProvider.getConnection()) {
            if (producto.getIdProducto() == 0) {
                // 1. Validar que no exista un producto con el mismo código de barras
                if (producto.getCodigoBarras() != null && !producto.getCodigoBarras().isBlank()) {
                    String cod = producto.getCodigoBarras().trim();
                    Producto existente = productoDAO.findByCodigoBarras(conn, cod);
                    if (existente != null) {
                        log.warn("[GLOBAL-SCAN] [GUARDAR] Conflicto de código duplicado: {}", cod);
                        throw new ServiceException("El producto con código de barras '" + cod + "' ya está registrado en la base de datos.");
                    }
                }

                // 2. Valores por defecto para campos obligatorios
                if (producto.getNombre() == null || producto.getNombre().isBlank()) {
                    producto.setNombre(producto.getCodigoBarras() != null ? "Producto " + producto.getCodigoBarras() : "Producto sin nombre");
                }
                if (producto.getPrecioNormal() == null) {
                    producto.setPrecioNormal(BigDecimal.ZERO);
                }
                if (producto.getPrecioDescuento() == null) {
                    producto.setPrecioDescuento(BigDecimal.ZERO);
                }
                if (producto.getCosto() == null) {
                    producto.setCosto(BigDecimal.ZERO);
                }
                if (producto.getStock() < 0) {
                    producto.setStock(0);
                }
                if (producto.getFechaCreacion() == null) {
                    producto.setFechaCreacion(LocalDateTime.now());
                }

                int nuevoId = productoDAO.insert(conn, producto);
                producto.setIdProducto(nuevoId);
                log.info("[GLOBAL-SCAN] [GUARDAR] Inserción exitosa. Nuevo id_producto={}", nuevoId);
            } else {
                producto.setFechaModificacion(LocalDateTime.now());
                productoDAO.update(conn, producto);
                log.info("[GLOBAL-SCAN] [GUARDAR] Actualización exitosa para id_producto={}", producto.getIdProducto());
            }
        } catch (SQLException e) {
            log.error("[GLOBAL-SCAN] [GUARDAR] Error SQL al persistir producto [código: {}]: {}",
                    producto.getCodigoBarras(), e.getMessage(), e);
            throw new ServiceException("Error al guardar el producto en base de datos: " + e.getMessage(), e);
        } catch (ServiceException e) {
            log.error("[GLOBAL-SCAN] [GUARDAR] Validación de negocio fallida: {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("[GLOBAL-SCAN] [GUARDAR] Excepción inesperada al guardar producto [código: {}]: {}",
                    producto.getCodigoBarras(), e.getMessage(), e);
            throw new ServiceException("Error inesperado al guardar el producto: " + e.getMessage(), e);
        }
    }

    /**
     * Elimina un producto por su identificador.
     */
    public void delete(int id) {
        try (Connection conn = connectionProvider.getConnection()) {
            productoDAO.delete(conn, id);
        } catch (SQLException e) {
            throw new ServiceException("Error al eliminar el producto", e);
        }
    }
}
