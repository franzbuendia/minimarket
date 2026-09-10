package com.lacanasta.util;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Clase de utilidad para obtener conexiones JDBC hacia la base de datos MySQL.
 *
 * <p>Lee los parámetros de conexión desde el archivo {@code db.properties}
 * ubicado en el classpath (src/main/resources). El patrón utilizado es un
 * acceso estático (singleton de configuración) para evitar recargar el
 * archivo de propiedades en cada conexión.</p>
 */
public final class DatabaseConnection {

    private static final String CONFIG_FILE = "/db.properties";
    private static final Properties PROPERTIES = new Properties();

    static {
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + CONFIG_FILE + " en el classpath");
            }
            PROPERTIES.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Error al cargar " + CONFIG_FILE + ": " + e.getMessage());
        }
    }

    private DatabaseConnection() {
    }

    /**
     * Obtiene una nueva conexión a la base de datos.
     *
     * @return una conexión activa hacia MySQL.
     * @throws SQLException si no es posible establecer la conexión.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
                PROPERTIES.getProperty("db.url"),
                PROPERTIES.getProperty("db.user"),
                PROPERTIES.getProperty("db.password")
        );
    }
}
