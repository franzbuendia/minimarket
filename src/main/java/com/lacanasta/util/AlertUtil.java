package com.lacanasta.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/**
 * Utilidad para mostrar cuadros de diálogo de JavaFX de forma consistente en toda la aplicación.
 */
public final class AlertUtil {

    private AlertUtil() {
    }

    /**
     * Muestra un diálogo informativo.
     *
     * @param titulo  título de la ventana.
     * @param mensaje texto del mensaje.
     */
    public static void mostrarInfo(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de error.
     *
     * @param titulo  título de la ventana.
     * @param mensaje texto del mensaje.
     */
    public static void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de advertencia.
     *
     * @param titulo  título de la ventana.
     * @param mensaje texto del mensaje.
     */
    public static void mostrarAdvertencia(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    /**
     * Muestra un diálogo de confirmación (Sí/No).
     *
     * @param titulo  título de la ventana.
     * @param mensaje texto de la pregunta.
     * @return {@code true} si el usuario confirma, {@code false} en caso contrario.
     */
    public static boolean mostrarConfirmacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        Optional<ButtonType> resultado = alert.showAndWait();
        return resultado.isPresent() && resultado.get() == ButtonType.OK;
    }
}
