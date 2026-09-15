package com.lacanasta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Representa un turno de caja (apertura y cierre).
 */
public class Caja {

    private int idCaja;
    private LocalDateTime fechaApertura;
    private LocalDateTime fechaCierre;
    private BigDecimal montoInicial;
    private BigDecimal montoFinal;
    private BigDecimal montoEsperado;
    private BigDecimal diferencia;
    private int usuarioApertura;
    private Integer usuarioCierre;
    private EstadoCaja estado;

    public Caja() {
    }

    public int getIdCaja() {
        return idCaja;
    }

    public void setIdCaja(int idCaja) {
        this.idCaja = idCaja;
    }

    public LocalDateTime getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDateTime fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public LocalDateTime getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(LocalDateTime fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public BigDecimal getMontoInicial() {
        return montoInicial;
    }

    public void setMontoInicial(BigDecimal montoInicial) {
        this.montoInicial = montoInicial;
    }

    public BigDecimal getMontoFinal() {
        return montoFinal;
    }

    public void setMontoFinal(BigDecimal montoFinal) {
        this.montoFinal = montoFinal;
    }

    public BigDecimal getMontoEsperado() {
        return montoEsperado;
    }

    public void setMontoEsperado(BigDecimal montoEsperado) {
        this.montoEsperado = montoEsperado;
    }

    public BigDecimal getDiferencia() {
        return diferencia;
    }

    public void setDiferencia(BigDecimal diferencia) {
        this.diferencia = diferencia;
    }

    public int getUsuarioApertura() {
        return usuarioApertura;
    }

    public void setUsuarioApertura(int usuarioApertura) {
        this.usuarioApertura = usuarioApertura;
    }

    public Integer getUsuarioCierre() {
        return usuarioCierre;
    }

    public void setUsuarioCierre(Integer usuarioCierre) {
        this.usuarioCierre = usuarioCierre;
    }

    public EstadoCaja getEstado() {
        return estado;
    }

    public void setEstado(EstadoCaja estado) {
        this.estado = estado;
    }
}
