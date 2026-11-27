package com.lacanasta.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Implementación de {@link ConnectionProvider} que obtiene conexiones JDBC
 * utilizando un pool de conexiones optimizado (HikariCP) leyendo los parámetros
 * desde {@code db.properties}.
 * Soporta además un Modo Prueba seguro usando una base de datos espejo ({@code minimarket_pruebas})
 * para probar flujos reales sin alterar jamás la información de la tienda.
 */
public class DatabaseConnection implements ConnectionProvider {

    private static final String CONFIG_FILE = "/db.properties";
    private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/minimarket?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String DB_PRUEBAS = "minimarket_pruebas";

    private static final Properties PROPERTIES = new Properties();
    private static final String BASE_URL;
    private static final String BASE_USER;
    private static final String BASE_PASSWORD;
    private static final String BASE_DB_NAME;

    private static volatile HikariDataSource dataSource;
    private static volatile boolean modoPruebaActivo = false;

    static {
        try (InputStream in = DatabaseConnection.class.getResourceAsStream(CONFIG_FILE)) {
            if (in == null) {
                throw new IllegalStateException("No se encontró " + CONFIG_FILE + " en el classpath");
            }
            PROPERTIES.load(in);

            BASE_URL = System.getProperty("db.url", PROPERTIES.getProperty("db.url", DEFAULT_URL));
            BASE_USER = System.getProperty("db.user", PROPERTIES.getProperty("db.user", "root"));
            BASE_PASSWORD = System.getProperty("db.password", PROPERTIES.getProperty("db.password", ""));
            BASE_DB_NAME = extraerNombreBaseDatos(BASE_URL);

            dataSource = crearPool(BASE_URL, "Minimarket-HikariPool");

            Runtime.getRuntime().addShutdownHook(new Thread(DatabaseConnection::closePool));
        } catch (Exception e) {
            throw new ExceptionInInitializerError("Error al inicializar pool de conexiones: " + e.getMessage());
        }
    }

    private static HikariDataSource crearPool(String jdbcUrl, String poolName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(BASE_USER);
        config.setPassword(BASE_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(300000); // 5 minutos
        config.setMaxLifetime(1800000); // 30 minutos
        config.setConnectionTimeout(10000); // 10 segundos
        config.setPoolName(poolName);
        return new HikariDataSource(config);
    }

    /**
     * Constructor por defecto.
     */
    public DatabaseConnection() {
    }

    /**
     * Obtiene una conexión activa desde el pool de conexiones actual
     * (tienda real o base de datos espejo si el Modo Prueba está activo).
     */
    @Override
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Indica si el Modo Prueba (base de datos espejo) está activo.
     */
    public static boolean isModoPruebaActivo() {
        return modoPruebaActivo;
    }

    /**
     * Devuelve el nombre de la base de datos actualmente conectada.
     */
    public static String getBaseDatosActiva() {
        return modoPruebaActivo ? DB_PRUEBAS : BASE_DB_NAME;
    }

    /**
     * Clona la estructura y datos de la base de datos real hacia {@code minimarket_pruebas}
     * y redirige el pool HikariCP a la copia de pruebas. La base real queda intacta.
     *
     * @throws SQLException si falla la creación o clonado de la base espejo.
     */
    public static synchronized void activarModoPrueba() throws SQLException {
        if (modoPruebaActivo) {
            return;
        }
        long t0 = System.currentTimeMillis();
        clonarBaseDatos(BASE_DB_NAME, DB_PRUEBAS);

        String urlPruebas = reemplazarNombreBaseDatos(BASE_URL, BASE_DB_NAME, DB_PRUEBAS);
        HikariDataSource nuevoPool = crearPool(urlPruebas, "Minimarket-Pruebas-Pool");
        HikariDataSource poolAnterior = dataSource;
        dataSource = nuevoPool;
        modoPruebaActivo = true;

        if (poolAnterior != null && !poolAnterior.isClosed()) {
            poolAnterior.close();
        }
        ResourceMonitor.registrarActividad("Activar Modo Prueba (" + DB_PRUEBAS + ")", System.currentTimeMillis() - t0);
    }

    /**
     * Desactiva el Modo Prueba y reconecta el pool a la base de datos real de la tienda.
     */
    public static synchronized void desactivarModoPrueba() {
        if (!modoPruebaActivo) {
            return;
        }
        long t0 = System.currentTimeMillis();
        HikariDataSource poolReal = crearPool(BASE_URL, "Minimarket-HikariPool");
        HikariDataSource poolPruebas = dataSource;
        dataSource = poolReal;
        modoPruebaActivo = false;

        if (poolPruebas != null && !poolPruebas.isClosed()) {
            poolPruebas.close();
        }
        ResourceMonitor.registrarActividad("Salir de Modo Prueba (" + BASE_DB_NAME + ")", System.currentTimeMillis() - t0);
    }

    private static void clonarBaseDatos(String origen, String destino) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute("DROP DATABASE IF EXISTS `" + destino + "`");
            stmt.execute("CREATE DATABASE `" + destino + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci");

            List<String> tablas = new ArrayList<>();
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE'")) {
                ps.setString(1, origen);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        tablas.add(rs.getString(1));
                    }
                }
            }

            stmt.execute("SET FOREIGN_KEY_CHECKS = 0");
            try {
                for (String tabla : tablas) {
                    String ddl = null;
                    try (ResultSet rs = stmt.executeQuery("SHOW CREATE TABLE `" + origen + "`.`" + tabla + "`")) {
                        if (rs.next()) {
                            ddl = rs.getString(2);
                        }
                    }
                    if (ddl != null) {
                        stmt.execute("USE `" + destino + "`");
                        stmt.execute(ddl);
                        stmt.execute("INSERT INTO `" + destino + "`.`" + tabla + "` SELECT * FROM `" + origen + "`.`" + tabla + "`");
                    }
                }
            } finally {
                stmt.execute("USE `" + origen + "`");
                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");
            }
        }
    }

    static String extraerNombreBaseDatos(String jdbcUrl) {
        String sinParametros = jdbcUrl.contains("?") ? jdbcUrl.substring(0, jdbcUrl.indexOf('?')) : jdbcUrl;
        int ultimoSlash = sinParametros.lastIndexOf('/');
        if (ultimoSlash >= 0 && ultimoSlash < sinParametros.length() - 1) {
            return sinParametros.substring(ultimoSlash + 1);
        }
        return "minimarket";
    }

    static String reemplazarNombreBaseDatos(String jdbcUrl, String origen, String destino) {
        int queryIdx = jdbcUrl.indexOf('?');
        String base = queryIdx >= 0 ? jdbcUrl.substring(0, queryIdx) : jdbcUrl;
        String query = queryIdx >= 0 ? jdbcUrl.substring(queryIdx) : "";
        int ultimoSlash = base.lastIndexOf('/');
        if (ultimoSlash >= 0) {
            return base.substring(0, ultimoSlash + 1) + destino + query;
        }
        return jdbcUrl.replace(origen, destino);
    }

    /**
     * Permite cerrar el pool de conexiones si se requiere manualmente.
     */
    public static void closePool() {
        HikariDataSource actual = dataSource;
        if (actual != null && !actual.isClosed()) {
            actual.close();
        }
    }
}
