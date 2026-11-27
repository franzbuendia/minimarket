package com.lacanasta.app;

import javafx.application.Application;
import com.lacanasta.util.ViewLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Clase principal que lanza la aplicación JavaFX.
 */
public class MainApp extends Application {

    /**
     * Punto de entrada de la aplicación.
     *
     * @param primaryStage la ventana principal.
     * @throws Exception si falla la carga de la vista de login.
     */
    @Override
    public void start(Stage primaryStage) throws Exception {
        Parent root = ViewLoader.load("/com/lacanasta/fxml/login.fxml");
        Scene scene = new Scene(root, 470, 630);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

        primaryStage.setTitle("La Canasta - Sistema POS & Gestión");
        primaryStage.setResizable(true);
        primaryStage.setMinWidth(470);
        primaryStage.setMinHeight(630);
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    @Override
    public void stop() {
        com.lacanasta.util.ResourceMonitor.detener();
    }

    /**
     * Método main de la aplicación.
     *
     * @param args argumentos de línea de comandos.
     */
    public static void main(String[] args) {
        initSentry();
        com.lacanasta.util.ResourceMonitor.iniciar();
        launch(args);
    }

    /**
     * Inicializa Sentry. El DSN se lee de la variable de entorno SENTRY_DSN
     * o del archivo sentry.properties (ver sentry.properties.example).
     * Si no hay DSN configurado, Sentry queda desactivado sin afectar la app.
     */
    private static void initSentry() {
        java.util.Properties props = new java.util.Properties();
        try (java.io.InputStream in = MainApp.class.getResourceAsStream("/sentry.properties")) {
            if (in != null) {
                props.load(in);
            }
        } catch (java.io.IOException ignored) {
            // sin archivo: se usa solo la variable de entorno
        }

        String dsn = System.getenv("SENTRY_DSN");
        if (dsn == null || dsn.isBlank()) {
            dsn = props.getProperty("dsn", "");
        }
        if (dsn.isBlank() || dsn.contains("<key>")) {
            return; // Sentry desactivado: la app funciona igual sin DSN
        }

        final String dsnFinal = dsn;
        final String entorno = System.getenv().getOrDefault("SENTRY_ENVIRONMENT",
                props.getProperty("environment", "development"));
        io.sentry.Sentry.init(options -> {
            options.setDsn(dsnFinal);
            options.setEnvironment(entorno);
            // Cola offline: si no hay internet (o la app se cierra a la fuerza), los eventos
            // se guardan en disco y se envian en el siguiente arranque con conexion.
            options.setCacheDirPath(java.nio.file.Paths
                    .get(System.getProperty("user.home"), ".lacanasta", "sentry-cache").toString());
            options.setMaxCacheItems(50);
            // Al iniciar, reenvia los eventos que quedaron guardados mientras no habia internet.
            options.addIntegration(new io.sentry.SendCachedEnvelopeFireAndForgetIntegration(
                    new io.sentry.SendFireAndForgetEnvelopeSender(options::getCacheDirPath)));
            // Localmente se muestrean todas las trazas; en producción solo el 10 %.
            options.setTracesSampleRate("production".equalsIgnoreCase(entorno) ? 0.1 : 1.0);
            options.getLogs().setEnabled(true);
        });
    }
}
