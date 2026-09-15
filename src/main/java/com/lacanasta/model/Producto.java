package com.lacanasta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un producto del inventario.
 *
 * <p>El precio de venta efectivo se calcula según la regla de negocio:
 * si {@code precio_descuento} es mayor que cero se usa ese precio; en caso
 * contrario se usa {@code precio_normal}.</p>
 */
public class Producto {

    private int idProducto;
    private String codigoBarras;
    private String nombre;
    private BigDecimal precioNormal;
    private BigDecimal precioDescuento;
    private int stock;
    private boolean estado;
    private Integer idCategoria;
    private Integer usuarioCreacion;
    private LocalDateTime fechaCreacion;
    private Integer usuarioModificacion;
    private LocalDateTime fechaModificacion;

    public Producto() {
    }

    /**
     * Devuelve el precio de venta efectivo aplicando la regla de descuento.
     *
     * @return {@code precio_descuento} si es mayor que cero; de lo contrario {@code precio_normal}.
     */
    public BigDecimal getPrecioVenta() {
        if (precioDescuento != null && precioDescuento.compareTo(BigDecimal.ZERO) > 0) {
            return precioDescuento;
        }
        return precioNormal;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecioNormal() {
        return precioNormal;
    }

    public void setPrecioNormal(BigDecimal precioNormal) {
        this.precioNormal = precioNormal;
    }

    public BigDecimal getPrecioDescuento() {
        return precioDescuento;
    }

    public void setPrecioDescuento(BigDecimal precioDescuento) {
        this.precioDescuento = precioDescuento;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public boolean isEstado() {
        return estado;
    }

    public void setEstado(boolean estado) {
        this.estado = estado;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Integer getUsuarioCreacion() {
        return usuarioCreacion;
    }

    public void setUsuarioCreacion(Integer usuarioCreacion) {
        this.usuarioCreacion = usuarioCreacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public Integer getUsuarioModificacion() {
        return usuarioModificacion;
    }

    public void setUsuarioModificacion(Integer usuarioModificacion) {
        this.usuarioModificacion = usuarioModificacion;
    }

    public LocalDateTime getFechaModificacion() {
        return fechaModificacion;
    }

    public void setFechaModificacion(LocalDateTime fechaModificacion) {
        this.fechaModificacion = fechaModificacion;
    }
}
