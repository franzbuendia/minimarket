package com.lacanasta.controller;

import com.lacanasta.model.Rol;
import com.lacanasta.model.Usuario;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.IconFactory;
import com.lacanasta.util.SessionManager;
import com.lacanasta.util.ViewLoader;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.util.List;

/**
 * Controlador del shell principal (sidebar + topbar) con navegación entre módulos.
 */
public class MainController {

    private static final Color NAV_ICON_COLOR = Color.web("#94a3b8");
    private static MainController instancia;

    @FXML
    private BorderPane rootPane;

    @FXML
    private StackPane contentArea;

    @FXML
    private Label lblModule;

    @FXML
    private Label lblUsuario;

    @FXML
    private Label lblRol;

    @FXML
    private Label lblAvatar;

    @FXML
    private Button btnLogout;

    @FXML
    private Button btnVentas;

    @FXML
    private Button btnCaja;

    @FXML
    private Button btnReportes;

    @FXML
    private Button btnProductos;

    @FXML
    private Button btnCompras;

    @FXML
    private Button btnProveedores;

    @FXML
    private Button btnUsuarios;

    @FXML
    private Button btnConfiguracion;

    private List<Button> navButtons;

    @FXML
    private void initialize() {
        instancia = this;

        navButtons = List.of(btnVentas, btnCaja, btnReportes, btnProductos,
                btnCompras, btnProveedores, btnUsuarios, btnConfiguracion);

        configurarNav(btnVentas, "Ventas", "sales");
        configurarNav(btnCaja, "Caja", "cash");
        configurarNav(btnReportes, "Reportes", "reports");
        configurarNav(btnProductos, "Productos", "products");
        configurarNav(btnCompras, "Compras", "purchases");
        configurarNav(btnProveedores, "Proveedores", "suppliers");
        configurarNav(btnUsuarios, "Usuarios", "users");
        configurarNav(btnConfiguracion, "Configuración", "settings");

        btnLogout.setText("Cerrar sesión");
        btnLogout.setGraphic(IconFactory.create("logout", 17, Color.web("#f1f5f9")));

        cargarUsuario();
        abrirVentas();
    }

    private void cargarUsuario() {
        Usuario usuario = SessionManager.getUsuarioActual();
        if (usuario != null) {
            lblUsuario.setText(usuario.getNombre());
            lblRol.setText(usuario.getRol().name());
            lblAvatar.setText(iniciales(usuario.getNombre()));

            boolean admin = usuario.getRol() == Rol.ADMIN;
            ocultarSiNoAdmin(btnProductos, admin);
            ocultarSiNoAdmin(btnCompras, admin);
            ocultarSiNoAdmin(btnProveedores, admin);
            ocultarSiNoAdmin(btnUsuarios, admin);
            ocultarSiNoAdmin(btnConfiguracion, admin);
        }
    }

    private String iniciales(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            return "US";
        }
        String[] partes = nombre.trim().split("\\s+");
        if (partes.length == 1) {
            return partes[0].substring(0, Math.min(2, partes[0].length())).toUpperCase();
        }
        return (partes[0].substring(0, 1) + partes[1].substring(0, 1)).toUpperCase();
    }

    private void configurarNav(Button boton, String texto, String icono) {
        boton.setText(texto);
        boton.setGraphic(IconFactory.create(icono, 20, NAV_ICON_COLOR));
    }

    private void ocultarSiNoAdmin(Button boton, boolean admin) {
        boton.setVisible(admin);
        boton.setManaged(admin);
    }

    @FXML
    private void abrirVentas() {
        navegar("/com/lacanasta/fxml/ventas.fxml", "Ventas", btnVentas);
    }

    @FXML
    private void abrirCaja() {
        navegar("/com/lacanasta/fxml/caja.fxml", "Caja", btnCaja);
    }

    @FXML
    private void abrirReportes() {
        navegar("/com/lacanasta/fxml/reportes.fxml", "Reportes", btnReportes);
    }

    @FXML
    private void abrirProductos() {
        navegar("/com/lacanasta/fxml/productos.fxml", "Productos", btnProductos);
    }

    @FXML
    private void abrirCompras() {
        navegar("/com/lacanasta/fxml/compras.fxml", "Compras", btnCompras);
    }

    @FXML
    private void abrirProveedores() {
        navegar("/com/lacanasta/fxml/proveedores.fxml", "Proveedores", btnProveedores);
    }

    @FXML
    private void abrirUsuarios() {
        navegar("/com/lacanasta/fxml/usuarios.fxml", "Usuarios", btnUsuarios);
    }

    @FXML
    private void abrirConfiguracion() {
        navegar("/com/lacanasta/fxml/configuracion.fxml", "Configuración", btnConfiguracion);
    }

    private void navegar(String fxml, String modulo, Button activo) {
        try {
            Parent view = ViewLoader.load(fxml);
            view.setOpacity(0.0);
            view.setTranslateY(8.0);
            contentArea.getChildren().setAll(view);

            FadeTransition fade = new FadeTransition(Duration.millis(220), view);
            fade.setFromValue(0.0);
            fade.setToValue(1.0);

            TranslateTransition slide = new TranslateTransition(Duration.millis(220), view);
            slide.setFromY(8.0);
            slide.setToY(0.0);

            ParallelTransition transition = new ParallelTransition(fade, slide);
            transition.play();

            lblModule.setText(modulo);
            for (Button b : navButtons) {
                b.getStyleClass().remove("active");
            }
            activo.getStyleClass().add("active");
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", "No se pudo cargar el módulo: " + e.getMessage());
        }
    }

    public static void irAProductos() {
        if (instancia != null) {
            instancia.abrirProductos();
        }
    }

    @FXML
    private void cerrarSesion() {
        try {
            SessionManager.clear();
            Parent root = ViewLoader.load("/com/lacanasta/fxml/login.fxml");
            Stage stage = (Stage) rootPane.getScene().getWindow();
            Scene scene = new Scene(root, 470, 630);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            stage.setTitle("La Canasta - Sistema POS & Gestión");
            stage.setMaximized(false);
            stage.setResizable(false);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", "No se pudo cerrar la sesión: " + e.getMessage());
        }
    }
}
