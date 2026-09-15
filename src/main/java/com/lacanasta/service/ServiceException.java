package com.lacanasta.service;

/**
 * Excepción de negocio lanzada por la capa de servicios.
 */
public class ServiceException extends RuntimeException {

    /**
     * Constructor con mensaje.
     */
    public ServiceException(String message) {
        super(message);
    }

    /**
     * Constructor con mensaje y causa.
     */
    public ServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
