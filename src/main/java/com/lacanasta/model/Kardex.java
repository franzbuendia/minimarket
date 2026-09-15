package com.lacanasta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un registro del Kardex de inventario (historial de movimientos).
 */
public class Kardex {

    private int idKardex;
    private int idProducto;
    private int idUsuarioMovimiento;
    private LocalDateTime fechaMovimiento;
    private TipoMovimiento tipoMovimiento;
    private String referencia;
    private int cantidad;
    private BigDecimal costoUnitario;
    private int stockAnterior;
    private int stockResultante;

    public Kardex() {
    }

    public int getIdKardex() {
        return idKardex;
    }

    public void setIdKardex(int idKardex) {
        this.idKardex = idKardex;
    }

    public int getIdProducto() {
        return idProducto;
    }

    public void setIdProducto(int idProducto) {
        this.idProducto = idProducto;
    }

    public int getIdUsuarioMovimiento() {
        return idUsuarioMovimiento;
    }

    public void setIdUsuarioMovimiento(int idUsuarioMovimiento) {
        this.idUsuarioMovimiento = idUsuarioMovimiento;
    }

    public LocalDateTime getFechaMovimiento() {
        return fechaMovimiento;
    }

    public void setFechaMovimiento(LocalDateTime fechaMovimiento) {
        this.fechaMovimiento = fechaMovimiento;
    }

    public TipoMovimiento getTipoMovimiento() {
        return tipoMovimiento;
    }

    public void setTipoMovimiento(TipoMovimiento tipoMovimiento) {
        this.tipoMovimiento = tipoMovimiento;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public BigDecimal getCostoUnitario() {
        return costoUnitario;
    }

    public void setCostoUnitario(BigDecimal costoUnitario) {
        this.costoUnitario = costoUnitario;
    }

    public int getStockAnterior() {
        return stockAnterior;
    }

    public void setStockAnterior(int stockAnterior) {
        this.stockAnterior = stockAnterior;
    }

    public int getStockResultante() {
        return stockResultante;
    }

    public void setStockResultante(int stockResultante) {
        this.stockResultante = stockResultante;
    }
}
