package com.lacanasta.dto;

import java.math.BigDecimal;

/**
 * Objeto de transferencia de datos (DTO) para la información de productos
 * detectados mediante escaneo y consulta a bases de datos peruanas.
 */
public class ProductoDTO {

    private String codigoBarras;
    private String nombre;
    private String marca;
    private String categoria;
    private String imagenUrl;
    private String imagenRuta;
    private String paises;

    // Banderas de validación de origen y disponibilidad
    private boolean prefijoPeruano;
    private boolean encontrado;
    private boolean vendidoEnPeru;

    // Valores editables por el cajero
    private BigDecimal precioVenta;
    private BigDecimal precioCompra;
    private Integer stock;

    public ProductoDTO() {
    }

    public ProductoDTO(String codigoBarras) {
        this.codigoBarras = codigoBarras;
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

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public String getImagenUrl() {
        return imagenUrl;
    }

    public void setImagenUrl(String imagenUrl) {
        this.imagenUrl = imagenUrl;
    }

    public String getImagenRuta() {
        return imagenRuta;
    }

    public void setImagenRuta(String imagenRuta) {
        this.imagenRuta = imagenRuta;
    }

    public String getPaises() {
        return paises;
    }

    public void setPaises(String paises) {
        this.paises = paises;
    }

    public boolean isPrefijoPeruano() {
        return prefijoPeruano;
    }

    public void setPrefijoPeruano(boolean prefijoPeruano) {
        this.prefijoPeruano = prefijoPeruano;
    }

    public boolean isEncontrado() {
        return encontrado;
    }

    public void setEncontrado(boolean encontrado) {
        this.encontrado = encontrado;
    }

    public boolean isVendidoEnPeru() {
        return vendidoEnPeru;
    }

    public void setVendidoEnPeru(boolean vendidoEnPeru) {
        this.vendidoEnPeru = vendidoEnPeru;
    }

    public BigDecimal getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getPrecioCompra() {
        return precioCompra;
    }

    public void setPrecioCompra(BigDecimal precioCompra) {
        this.precioCompra = precioCompra;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    @Override
    public String toString() {
        return "ProductoDTO{" +
                "codigoBarras='" + codigoBarras + '\'' +
                ", nombre='" + nombre + '\'' +
                ", marca='" + marca + '\'' +
                ", prefijoPeruano=" + prefijoPeruano +
                ", encontrado=" + encontrado +
                ", vendidoEnPeru=" + vendidoEnPeru +
                '}';
    }
}
