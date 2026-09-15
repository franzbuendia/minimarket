package com.lacanasta.util;

import com.lacanasta.model.Usuario;

/**
 * Administrador de sesión de la aplicación.
 *
 * <p>Guarda el usuario autenticado actualmente para que el resto de módulos
 * puedan conocer quién opera el sistema y su rol. Es un acceso estático por
 * ser una aplicación de escritorio de un solo usuario a la vez.</p>
 */
public final class SessionManager {

    private static Usuario usuarioActual;

    private SessionManager() {
    }

    /**
     * Devuelve el usuario autenticado actualmente.
     *
     * @return el usuario en sesión, o {@code null} si no hay sesión iniciada.
     */
    public static Usuario getUsuarioActual() {
        return usuarioActual;
    }

    /**
     * Establece el usuario autenticado.
     *
     * @param usuario el usuario que inicia sesión.
     */
    public static void setUsuarioActual(Usuario usuario) {
        usuarioActual = usuario;
    }

    /**
     * Cierra la sesión actual limpiando el usuario almacenado.
     */
    public static void clear() {
        usuarioActual = null;
    }
}
