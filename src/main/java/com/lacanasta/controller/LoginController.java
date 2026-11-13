package com.lacanasta.controller;

import com.lacanasta.model.Usuario;
import com.lacanasta.service.UsuarioService;
import com.lacanasta.util.SessionManager;
import com.lacanasta.util.ViewLoader;
import javafx.fxml.FXML;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.InputStream;

/**
 * Controlador de la vista de login con soporte para revelar contraseña
 * y acceso rápido para cuentas de cajero y administrador.
 */
public class LoginController {

    private final UsuarioService usuarioService;

    public LoginController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    private TextField txtUsuario;

    @FXML
    private PasswordField txtContrasena;

    @FXML
    private TextField txtContrasenaPlano;

    @FXML
    private Button btnTogglePassword;

    @FXML
    private Label lblError;

    @FXML
    private ImageView imgLogo;

    private boolean mostrandoContrasena = false;

    /**
     * Inicializa la vista cargando el logo y sincronizando los campos de contraseña.
     */
    @FXML
    private void initialize() {
        try (InputStream is = getClass().getResourceAsStream("/images/logo-supermercado.png")) {
            if (is != null) {
                imgLogo.setImage(new Image(is));
            } else {
                try (InputStream isFallback = getClass().getResourceAsStream("/images/logo-principal.jpg")) {
                    if (isFallback != null) {
                        imgLogo.setImage(new Image(isFallback));
                    }
                }
            }
        } catch (Exception ignored) {
        }

        if (txtContrasena != null && txtContrasenaPlano != null) {
            txtContrasenaPlano.textProperty().bindBidirectional(txtContrasena.textProperty());
            txtContrasenaPlano.setVisible(false);
            txtContrasenaPlano.setManaged(false);
        }

        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    @FXML
    private void onTogglePassword() {
        mostrandoContrasena = !mostrandoContrasena;
        if (txtContrasena != null && txtContrasenaPlano != null) {
            txtContrasena.setVisible(!mostrandoContrasena);
            txtContrasena.setManaged(!mostrandoContrasena);
            txtContrasenaPlano.setVisible(mostrandoContrasena);
            txtContrasenaPlano.setManaged(mostrandoContrasena);

            if (btnTogglePassword != null) {
                FontIcon icon = new FontIcon(mostrandoContrasena ? "fas-eye-slash" : "fas-eye");
                icon.setIconSize(15);
                icon.setIconColor(javafx.scene.paint.Color.web("#64748b"));
                btnTogglePassword.setGraphic(icon);
            }
        }
    }

    @FXML
    private void onSelectAdmin() {
        txtUsuario.setText("admin");
        txtContrasena.setText("admin123");
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    @FXML
    private void onSelectCajero() {
        txtUsuario.setText("cajero1");
        txtContrasena.setText("cajero123");
        if (lblError != null) {
            lblError.setVisible(false);
            lblError.setManaged(false);
        }
    }

    /**
     * Maneja el evento de inicio de sesión.
     */
    @FXML
    private void onLogin() {
        String username = txtUsuario != null ? txtUsuario.getText().trim() : "";
        String password = txtContrasena != null ? txtContrasena.getText() : "";

        if (username.isEmpty() || password.isEmpty()) {
            mostrarError("Ingrese usuario y contraseña");
            return;
        }

        Usuario usuario;
        try {
            usuario = usuarioService.authenticate(username, password);
        } catch (Exception e) {
            mostrarError("Error al conectar a MySQL: " + e.getMessage());
            return;
        }

        if (usuario == null) {
            mostrarError("Credenciales incorrectas (usuario o contraseña inválidos)");
            return;
        }

        try {
            SessionManager.setUsuarioActual(usuario);
            abrirPantallaPrincipal();
        } catch (Exception e) {
            mostrarError("Error al iniciar la vista principal: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void abrirPantallaPrincipal() throws Exception {
        Parent root = ViewLoader.load("/com/lacanasta/fxml/main-view.fxml");
        Stage stage = (Stage) txtUsuario.getScene().getWindow();
        Scene scene = new Scene(root, 1200, 780);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        stage.setTitle("La Canasta - Sistema POS & Gestión");
        stage.setResizable(true);
        stage.setScene(scene);
        stage.setMaximized(true);
    }

    private void mostrarError(String mensaje) {
        if (lblError != null) {
            lblError.setText(mensaje);
            lblError.setVisible(true);
            lblError.setManaged(true);
        }
    }
}
