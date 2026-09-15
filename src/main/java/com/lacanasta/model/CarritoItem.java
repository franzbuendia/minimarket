package com.lacanasta.model;

import java.math.BigDecimal;

/**
 * Ítem del carrito de venta: un producto y su cantidad.
 *
 * <p>El precio y subtotal se calculan dinámicamente usando
 * {@link Producto#getPrecioVenta()} para aplicar la regla de descuento.</p>
 */
public class CarritoItem {

    private final Producto producto;
    private int cantidad;

    /**
     * Constructor.
     *
     * @param producto producto agregado al carrito.
     * @param cantidad cantidad inicial.
     */
    public CarritoItem(Producto producto, int cantidad) {
        this.producto = producto;
        this.cantidad = cantidad;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getIdProducto() {
        return producto.getIdProducto();
    }

    public String getNombre() {
        return producto.getNombre();
    }

    /**
     * Devuelve el precio unitario efectivo (con descuento si aplica).
     */
    public BigDecimal getPrecio() {
        return producto.getPrecioVenta();
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    /**
     * Devuelve el subtotal de la línea (precio × cantidad).
     */
    public BigDecimal getSubtotal() {
        return producto.getPrecioVenta().multiply(BigDecimal.valueOf(cantidad));
    }
}
