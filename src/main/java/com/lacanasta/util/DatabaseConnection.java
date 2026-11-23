package com.lacanasta.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Implementación de {@link ConnectionProvider} que obtiene conexiones JDBC
 * utilizando un pool de conexiones optimizado (HikariCP) leyendo los parámetros
 * desde {@code db.properties}.
 */
public class DatabaseConnection implements ConnectionProvider {

    private static final String CONFIG_FILE = "/db.properties";
    private static final Properties PROPERTIES = new Properties();
    private static final HikariDataSource DATA_SOURCE;

    static {
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + CONFIG_FILE + " en el classpath");
            }
            PROPERTIES.load(in);

            String url = System.getProperty("db.url",
                    PROPERTIES.getProperty("db.url", "jdbc:mysql://localhost:3306/minimarket?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"));
            String user = System.getProperty("db.user",
                    PROPERTIES.getProperty("db.user", "root"));
            String password = System.getProperty("db.password",
                    PROPERTIES.getProperty("db.password", ""));

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(url);
            config.setUsername(user);
            config.setPassword(password);

            // Pool settings optimizados para aplicación de escritorio POS conectada a la nube
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000); // 5 minutos
            config.setMaxLifetime(1800000); // 30 minutos
            config.setConnectionTimeout(10000); // 10 segundos
            config.setPoolName("Minimarket-HikariPool");

            DATA_SOURCE = new HikariDataSource(config);

            // Registrar ShutdownHook para cerrar el pool limpiamente al salir
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
                    DATA_SOURCE.close();
                }
            }));
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Error al inicializar pool de conexiones: " + e.getMessage());
        }
    }

    /**
     * Constructor por defecto.
     */
    public DatabaseConnection() {
    }

    /**
     * Obtiene una conexión activa desde el pool de conexiones.
     *
     * @return una conexión activa hacia MySQL.
     * @throws SQLException si no es posible obtener una conexión del pool.
     */
    @Override
    public Connection getConnection() throws SQLException {
        return DATA_SOURCE.getConnection();
    }

    /**
     * Permite cerrar el pool de conexiones si se requiere manualmente.
     */
    public static void closePool() {
        if (DATA_SOURCE != null && !DATA_SOURCE.isClosed()) {
            DATA_SOURCE.close();
        }
    }
}
