package com.lacanasta.dao.jdbc;

import com.lacanasta.dao.ProductoDAO;

import com.lacanasta.model.Producto;
import com.lacanasta.util.JdbcUtil;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Acceso a datos de la tabla {@code productos}.
 */
public class ProductoDAOJdbc implements ProductoDAO {

    private static final String COLUMNS =
            "id_producto, codigo_barras, nombre, marca, precio_normal, precio_descuento, costo, stock, estado, "
            + "id_categoria, categoria_nombre, imagen_url, imagen_ruta, usuario_creacion, fecha_creacion, usuario_modificacion, fecha_modificacion";

    /**
     * Busca un producto por su identificador.
     */
    public Producto findById(Connection conn, int id) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Busca un producto por su código de barras.
     */
    public Producto findByCodigoBarras(Connection conn, String codigoBarras) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos WHERE codigo_barras = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, codigoBarras);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * Lista todos los productos ordenados por nombre.
     */
    public List<Producto> findAll(Connection conn) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos ORDER BY nombre";
        List<Producto> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(map(rs));
            }
        }
        return list;
    }

    /**
     * Busca productos cuyo nombre coincida parcialmente con el texto dado.
     */
    public List<Producto> findByNombre(Connection conn, String nombre) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos WHERE nombre LIKE ? ORDER BY nombre";
        List<Producto> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + nombre + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Lista los productos que pertenecen a una categoría.
     */
    public List<Producto> findByCategoria(Connection conn, int idCategoria) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos WHERE id_categoria = ? ORDER BY nombre";
        List<Producto> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCategoria);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Lista los productos con stock igual o menor al límite indicado (para alertas).
     */
    public List<Producto> findByStockBajo(Connection conn, int limite) throws SQLException {
        String sql = "SELECT " + COLUMNS + " FROM productos WHERE stock <= ? AND estado = 1 ORDER BY stock";
        List<Producto> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limite);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(map(rs));
                }
            }
        }
        return list;
    }

    /**
     * Inserta un producto y devuelve su id generado.
     */
    public int insert(Connection conn, Producto p) throws SQLException {
        String sql = "INSERT INTO productos (codigo_barras, nombre, marca, precio_normal, precio_descuento, costo, stock, estado, "
                + "id_categoria, categoria_nombre, imagen_url, imagen_ruta, usuario_creacion, fecha_creacion) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            String cod = p.getCodigoBarras() != null ? p.getCodigoBarras().trim() : null;
            if (cod != null && cod.length() > 50) {
                cod = cod.substring(0, 50);
            }
            String nom = p.getNombre() != null ? p.getNombre().trim() : "";
            if (nom.length() > 100) {
                nom = nom.substring(0, 100);
            }
            String mar = p.getMarca() != null ? p.getMarca().trim() : null;
            if (mar != null && mar.length() > 100) {
                mar = mar.substring(0, 100);
            }
            String catNom = p.getCategoriaNombre() != null ? p.getCategoriaNombre().trim() : null;
            if (catNom != null && catNom.length() > 100) {
                catNom = catNom.substring(0, 100);
            }
            String imgUrl = p.getImagenUrl() != null ? p.getImagenUrl().trim() : null;
            if (imgUrl != null && imgUrl.length() > 255) {
                imgUrl = imgUrl.substring(0, 255);
            }
            String imgRuta = p.getImagenRuta() != null ? p.getImagenRuta().trim() : null;
            if (imgRuta != null && imgRuta.length() > 255) {
                imgRuta = imgRuta.substring(0, 255);
            }

            ps.setString(1, cod);
            ps.setString(2, nom);
            ps.setString(3, mar);
            ps.setBigDecimal(4, p.getPrecioNormal() != null ? p.getPrecioNormal() : BigDecimal.ZERO);
            ps.setBigDecimal(5, p.getPrecioDescuento() != null ? p.getPrecioDescuento() : BigDecimal.ZERO);
            ps.setBigDecimal(6, p.getCosto() != null ? p.getCosto() : BigDecimal.ZERO);
            ps.setInt(7, Math.max(0, p.getStock()));
            ps.setBoolean(8, p.isEstado());
            JdbcUtil.setInteger(ps, 9, p.getIdCategoria());
            ps.setString(10, catNom);
            ps.setString(11, imgUrl);
            ps.setString(12, imgRuta);
            JdbcUtil.setInteger(ps, 13, p.getUsuarioCreacion());
            JdbcUtil.setLocalDateTime(ps, 14, p.getFechaCreacion() != null ? p.getFechaCreacion() : java.time.LocalDateTime.now());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    p.setIdProducto(id);
                    return id;
                }
                return 0;
            }
        }
    }

    /**
     * Actualiza los datos de un producto existente.
     */
    public int update(Connection conn, Producto p) throws SQLException {
        String sql = "UPDATE productos SET codigo_barras = ?, nombre = ?, marca = ?, precio_normal = ?, precio_descuento = ?, costo = ?, "
                + "stock = ?, estado = ?, id_categoria = ?, categoria_nombre = ?, imagen_url = ?, imagen_ruta = ?, "
                + "usuario_modificacion = ?, fecha_modificacion = ? WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String cod = p.getCodigoBarras() != null ? p.getCodigoBarras().trim() : null;
            if (cod != null && cod.length() > 50) {
                cod = cod.substring(0, 50);
            }
            String nom = p.getNombre() != null ? p.getNombre().trim() : "";
            if (nom.length() > 100) {
                nom = nom.substring(0, 100);
            }
            String mar = p.getMarca() != null ? p.getMarca().trim() : null;
            if (mar != null && mar.length() > 100) {
                mar = mar.substring(0, 100);
            }
            String catNom = p.getCategoriaNombre() != null ? p.getCategoriaNombre().trim() : null;
            if (catNom != null && catNom.length() > 100) {
                catNom = catNom.substring(0, 100);
            }
            String imgUrl = p.getImagenUrl() != null ? p.getImagenUrl().trim() : null;
            if (imgUrl != null && imgUrl.length() > 255) {
                imgUrl = imgUrl.substring(0, 255);
            }
            String imgRuta = p.getImagenRuta() != null ? p.getImagenRuta().trim() : null;
            if (imgRuta != null && imgRuta.length() > 255) {
                imgRuta = imgRuta.substring(0, 255);
            }

            ps.setString(1, cod);
            ps.setString(2, nom);
            ps.setString(3, mar);
            ps.setBigDecimal(4, p.getPrecioNormal() != null ? p.getPrecioNormal() : BigDecimal.ZERO);
            ps.setBigDecimal(5, p.getPrecioDescuento() != null ? p.getPrecioDescuento() : BigDecimal.ZERO);
            ps.setBigDecimal(6, p.getCosto() != null ? p.getCosto() : BigDecimal.ZERO);
            ps.setInt(7, Math.max(0, p.getStock()));
            ps.setBoolean(8, p.isEstado());
            JdbcUtil.setInteger(ps, 9, p.getIdCategoria());
            ps.setString(10, catNom);
            ps.setString(11, imgUrl);
            ps.setString(12, imgRuta);
            JdbcUtil.setInteger(ps, 13, p.getUsuarioModificacion());
            JdbcUtil.setLocalDateTime(ps, 14, p.getFechaModificacion());
            ps.setInt(15, p.getIdProducto());
            return ps.executeUpdate();
        }
    }

    /**
     * Actualiza únicamente el stock de un producto.
     */
    public int updateStock(Connection conn, int idProducto, int nuevoStock) throws SQLException {
        String sql = "UPDATE productos SET stock = ? WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, nuevoStock);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        }
    }

    /**
     * Actualiza únicamente el costo de un producto.
     */
    public int updateCosto(Connection conn, int idProducto, BigDecimal costo) throws SQLException {
        String sql = "UPDATE productos SET costo = ? WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, costo);
            ps.setInt(2, idProducto);
            return ps.executeUpdate();
        }
    }

    /**
     * Actualiza stock, costo y auditoría de modificación de un producto.
     */
    public int updateStockCosto(Connection conn, int idProducto, int stock, BigDecimal costo, Integer usuarioModificacion) throws SQLException {
        String sql = "UPDATE productos SET stock = ?, costo = ?, usuario_modificacion = ?, fecha_modificacion = ? "
                + "WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, stock);
            ps.setBigDecimal(2, costo);
            JdbcUtil.setInteger(ps, 3, usuarioModificacion);
            JdbcUtil.setLocalDateTime(ps, 4, LocalDateTime.now());
            ps.setInt(5, idProducto);
            return ps.executeUpdate();
        }
    }

    /**
     * Elimina un producto por su identificador.
     */
    public int delete(Connection conn, int id) throws SQLException {
        String sql = "DELETE FROM productos WHERE id_producto = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate();
        }
    }

    private static Producto map(ResultSet rs) throws SQLException {
        Producto p = new Producto();
        p.setIdProducto(rs.getInt("id_producto"));
        p.setCodigoBarras(rs.getString("codigo_barras"));
        p.setNombre(rs.getString("nombre"));
        p.setMarca(rs.getString("marca"));
        p.setPrecioNormal(rs.getBigDecimal("precio_normal"));
        p.setPrecioDescuento(rs.getBigDecimal("precio_descuento"));
        p.setCosto(rs.getBigDecimal("costo"));
        p.setStock(rs.getInt("stock"));
        p.setEstado(rs.getBoolean("estado"));
        p.setIdCategoria(JdbcUtil.getInteger(rs, "id_categoria"));
        p.setCategoriaNombre(rs.getString("categoria_nombre"));
        p.setImagenUrl(rs.getString("imagen_url"));
        p.setImagenRuta(rs.getString("imagen_ruta"));
        p.setUsuarioCreacion(JdbcUtil.getInteger(rs, "usuario_creacion"));
        p.setFechaCreacion(JdbcUtil.getLocalDateTime(rs, "fecha_creacion"));
        p.setUsuarioModificacion(JdbcUtil.getInteger(rs, "usuario_modificacion"));
        p.setFechaModificacion(JdbcUtil.getLocalDateTime(rs, "fecha_modificacion"));
        return p;
    }
}
