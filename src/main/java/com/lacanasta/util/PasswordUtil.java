package com.lacanasta.util;

import org.mindrot.jbcrypt.BCrypt;

/**
 * Utilidad para el hasheo y verificación de contraseñas mediante BCrypt.
 *
 * <p>Permite trabajar de forma transparente tanto con contraseñas hasheadas
 * (formato {@code $2a$...}) como con contraseñas en texto plano durante la
 * fase de pruebas.</p>
 */
public final class PasswordUtil {

    private PasswordUtil() {
    }

    /**
     * Genera un hash BCrypt de una contraseña en texto plano.
     *
     * @param textoPlano la contraseña original.
     * @return el hash BCrypt generado.
     */
    public static String hash(String textoPlano) {
        return BCrypt.hashpw(textoPlano, BCrypt.gensalt(10));
    }

    /**
     * Verifica si una contraseña en texto plano coincide con un valor almacenado.
     *
     * <p>Si el valor almacenado es un hash BCrypt, usa {@link BCrypt#checkpw};
     * en caso contrario realiza una comparación directa (soporte para texto plano
     * durante las pruebas).</p>
     *
     * @param textoPlano la contraseña ingresada.
     * @param almacenado el valor guardado (hash o texto plano).
     * @return {@code true} si coinciden.
     */
    public static boolean verificar(String textoPlano, String almacenado) {
        if (almacenado == null) {
            return false;
        }
        if (almacenado.startsWith("$2a$") || almacenado.startsWith("$2b$") || almacenado.startsWith("$2y$")) {
            try {
                return BCrypt.checkpw(textoPlano, almacenado);
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
        return almacenado.equals(textoPlano);
    }
}
