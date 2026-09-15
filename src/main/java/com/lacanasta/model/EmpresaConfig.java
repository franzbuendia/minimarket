package com.lacanasta.model;

/**
 * Configuración de la empresa (datos para tickets y reportes).
 *
 * <p>Es una tabla singleton: siempre se usa la fila con {@code id_config = 1}.</p>
 */
public class EmpresaConfig {

    private int idConfig;
    private String nombreComercial;
    private String ruc;
    private String direccion;
    private String telefono;
    private String mensajeTicket;

    public EmpresaConfig() {
    }

    public int getIdConfig() {
        return idConfig;
    }

    public void setIdConfig(int idConfig) {
        this.idConfig = idConfig;
    }

    public String getNombreComercial() {
        return nombreComercial;
    }

    public void setNombreComercial(String nombreComercial) {
        this.nombreComercial = nombreComercial;
    }

    public String getRuc() {
        return ruc;
    }

    public void setRuc(String ruc) {
        this.ruc = ruc;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getMensajeTicket() {
        return mensajeTicket;
    }

    public void setMensajeTicket(String mensajeTicket) {
        this.mensajeTicket = mensajeTicket;
    }
}
