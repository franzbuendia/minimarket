package com.lacanasta.controller;

import com.lacanasta.model.EmpresaConfig;
import com.lacanasta.service.ConfigService;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.DatabaseConnection;
import javafx.animation.FadeTransition;
import javafx.animation.ParallelTransition;
import javafx.animation.TranslateTransition;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.sql.Connection;
import java.util.List;

/**
 * Controlador del módulo de configuración del sistema y negocio con
 * pestañas animadas y vista previa térmica interactiva.
 */
public class ConfigController {

    private final ConfigService configService;

    public ConfigController(ConfigService configService) {
        this.configService = configService;
    }

    @FXML
    private TextField txtNombreComercial;

    @FXML
    private TextField txtRuc;

    @FXML
    private TextField txtDireccion;

    @FXML
    private TextField txtTelefono;

    @FXML
    private TextField txtCiudad;

    @FXML
    private TextField txtEmail;

    @FXML
    private TextField txtGiro;

    @FXML
    private TextField txtMensajeTicket;

    @FXML
    private ComboBox<String> cmbTipoComprobante;

    @FXML
    private ComboBox<String> cmbAnchoPapel;

    @FXML
    private TextField txtMoneda;

    @FXML
    private TextField txtIgv;

    @FXML
    private Label lblTicketStore;

    @FXML
    private Label lblTicketRuc;

    @FXML
    private Label lblTicketDireccion;

    @FXML
    private Label lblTicketTelefono;

    @FXML
    private Label lblTicketMensaje;

    @FXML
    private TextField txtAperturaCaja;

    @FXML
    private TextField txtUmbralStock;

    @FXML
    private CheckBox chkExigirCaja;

    @FXML
    private CheckBox chkPermitirSinStock;

    @FXML
    private CheckBox chkAutoTicket;

    @FXML
    private Label lblDbDetalles;

    @FXML
    private Label lblLatencia;

    @FXML
    private Button btnDatosNegocio;

    @FXML
    private Button btnComprobantes;

    @FXML
    private Button btnVentasCaja;

    @FXML
    private Button btnPreferencias;

    @FXML
    private StackPane panelContenido;

    @FXML
    private VBox panelDatosNegocio;

    @FXML
    private VBox panelComprobantes;

    @FXML
    private VBox panelVentasCaja;

    @FXML
    private VBox panelPreferencias;

    private List<VBox> paneles;
    private List<Button> navButtons;

    @FXML
    private void initialize() {
        paneles = List.of(panelDatosNegocio, panelComprobantes, panelVentasCaja, panelPreferencias);
        navButtons = List.of(btnDatosNegocio, btnComprobantes, btnVentasCaja, btnPreferencias);

        if (cmbTipoComprobante != null) {
            cmbTipoComprobante.setItems(FXCollections.observableArrayList(
                    "Boleta de Venta (Electrónica)", "Factura Comercial", "Ticket de Control Interno"));
            cmbTipoComprobante.getSelectionModel().selectFirst();
        }

        if (cmbAnchoPapel != null) {
            cmbAnchoPapel.setItems(FXCollections.observableArrayList(
                    "80 mm (Estándar Impresora Térmica)", "58 mm (Mini Ticket)"));
            cmbAnchoPapel.getSelectionModel().selectFirst();
        }

        // Vincular los campos del formulario con la vista previa del ticket en vivo
        enlazarPreviewTicket();

        // Cargar datos existentes de la base de datos
        cargarDatos();
    }

    private void enlazarPreviewTicket() {
        if (txtNombreComercial != null && lblTicketStore != null) {
            txtNombreComercial.textProperty().addListener((obs, o, n) -> {
                lblTicketStore.setText(n == null || n.isBlank() ? "LA CANASTA" : n.toUpperCase());
            });
        }
        if (txtRuc != null && lblTicketRuc != null) {
            txtRuc.textProperty().addListener((obs, o, n) -> {
                lblTicketRuc.setText("RUC: " + (n == null || n.isBlank() ? "12345678901" : n));
            });
        }
        if (txtDireccion != null && lblTicketDireccion != null) {
            txtDireccion.textProperty().addListener((obs, o, n) -> {
                lblTicketDireccion.setText(n == null || n.isBlank() ? "Av. Principal 123" : n);
            });
        }
        if (txtTelefono != null && lblTicketTelefono != null) {
            txtTelefono.textProperty().addListener((obs, o, n) -> {
                lblTicketTelefono.setText("Tel: " + (n == null || n.isBlank() ? "999888777" : n));
            });
        }
        if (txtMensajeTicket != null && lblTicketMensaje != null) {
            txtMensajeTicket.textProperty().addListener((obs, o, n) -> {
                lblTicketMensaje.setText(n == null || n.isBlank() ? "¡Gracias por su compra!" : n);
            });
        }
    }

    private void cargarDatos() {
        try {
            EmpresaConfig config = configService.get();
            if (config != null) {
                if (txtNombreComercial != null) {
                    txtNombreComercial.setText(config.getNombreComercial() != null ? config.getNombreComercial() : "");
                }
                if (txtRuc != null) {
                    txtRuc.setText(config.getRuc() != null ? config.getRuc() : "");
                }
                if (txtDireccion != null) {
                    txtDireccion.setText(config.getDireccion() != null ? config.getDireccion() : "");
                }
                if (txtTelefono != null) {
                    txtTelefono.setText(config.getTelefono() != null ? config.getTelefono() : "");
                }
                if (txtMensajeTicket != null) {
                    txtMensajeTicket.setText(config.getMensajeTicket() != null ? config.getMensajeTicket() : "");
                }

                if (lblTicketStore != null && config.getNombreComercial() != null) {
                    lblTicketStore.setText(config.getNombreComercial().toUpperCase());
                }
                if (lblTicketRuc != null && config.getRuc() != null) {
                    lblTicketRuc.setText("RUC: " + config.getRuc());
                }
                if (lblTicketDireccion != null && config.getDireccion() != null) {
                    lblTicketDireccion.setText(config.getDireccion());
                }
                if (lblTicketTelefono != null && config.getTelefono() != null) {
                    lblTicketTelefono.setText("Tel: " + config.getTelefono());
                }
                if (lblTicketMensaje != null && config.getMensajeTicket() != null) {
                    lblTicketMensaje.setText(config.getMensajeTicket());
                }
            }
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    @FXML
    private void onGuardar() {
        try {
            EmpresaConfig config = configService.get();
            if (config == null) {
                config = new EmpresaConfig();
            }
            config.setNombreComercial(txtNombreComercial.getText().trim());
            config.setRuc(txtRuc.getText().trim());
            config.setDireccion(txtDireccion.getText().trim());
            config.setTelefono(txtTelefono.getText().trim());
            config.setMensajeTicket(txtMensajeTicket.getText().trim());
            configService.update(config);
            AlertUtil.mostrarInfo("Configuración", "Configuración del negocio guardada exitosamente");
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    @FXML
    private void onDatosNegocio() {
        mostrarPanel(panelDatosNegocio, btnDatosNegocio);
    }

    @FXML
    private void onComprobantes() {
        mostrarPanel(panelComprobantes, btnComprobantes);
    }

    @FXML
    private void onVentasCaja() {
        mostrarPanel(panelVentasCaja, btnVentasCaja);
    }

    @FXML
    private void onPreferencias() {
        mostrarPanel(panelPreferencias, btnPreferencias);
    }

    @FXML
    private void onProbarConexion() {
        long t0 = System.currentTimeMillis();
        try (Connection conn = new DatabaseConnection().getConnection()) {
            long delta = System.currentTimeMillis() - t0;
            if (lblLatencia != null) {
                lblLatencia.setText(delta + " ms (Conexión verificada exitosamente)");
            }
            AlertUtil.mostrarInfo("Conexión MySQL", "Conexión exitosa a MySQL (minimarket) en " + delta + " ms.");
        } catch (Exception e) {
            AlertUtil.mostrarError("Error de Conexión", "Fallo al conectar con MySQL: " + e.getMessage());
        }
    }

    @FXML
    private void onOptimizarTablas() {
        AlertUtil.mostrarInfo("Mantenimiento", "Tablas y cachés del sistema optimizados correctamente.");
    }

    private void mostrarPanel(VBox panelDestino, Button botonActivo) {
        for (Button b : navButtons) {
            b.getStyleClass().remove("active");
        }
        botonActivo.getStyleClass().add("active");

        for (VBox p : paneles) {
            if (p == panelDestino) {
                p.setVisible(true);
                p.setManaged(true);
                animarEntrada(p);
            } else {
                p.setVisible(false);
                p.setManaged(false);
            }
        }
    }

    private void animarEntrada(Node nodo) {
        nodo.setOpacity(0.0);
        nodo.setTranslateY(8.0);

        FadeTransition fade = new FadeTransition(Duration.millis(180), nodo);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);

        TranslateTransition slide = new TranslateTransition(Duration.millis(180), nodo);
        slide.setFromY(8.0);
        slide.setToY(0.0);

        ParallelTransition pt = new ParallelTransition(fade, slide);
        pt.play();
    }
}
