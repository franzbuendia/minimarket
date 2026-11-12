package com.lacanasta.controller;

import com.lacanasta.model.Caja;
import com.lacanasta.model.MovimientoCaja;
import com.lacanasta.model.RegistroCaja;
import com.lacanasta.model.TipoMovimientoCaja;
import com.lacanasta.model.Usuario;
import com.lacanasta.model.Venta;
import com.lacanasta.service.CajaService;
import com.lacanasta.service.UsuarioService;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.SessionManager;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.layout.Region;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Controlador del módulo de caja (apertura, cierre y movimientos).
 */
public class CajaController {

    private static final DateTimeFormatter FMT_HORA = DateTimeFormatter.ofPattern("HH:mm");

    private final CajaService cajaService;
    private final UsuarioService usuarioService;

    public CajaController(CajaService cajaService, UsuarioService usuarioService) {
        this.cajaService = cajaService;
        this.usuarioService = usuarioService;
    }

    @FXML
    private javafx.scene.layout.HBox panelEstado;

    @FXML
    private Button btnAbrirCaja;

    @FXML
    private Button btnCerrarCaja;

    @FXML
    private Button btnRegistrarIngreso;

    @FXML
    private Button btnRegistrarEgreso;

    @FXML
    private Label lblEstadoCaja;

    @FXML
    private Label lblFechaApertura;

    @FXML
    private Label lblResumen;

    @FXML
    private Label lblMetricaInicial;

    @FXML
    private Label lblMetricaVentas;

    @FXML
    private Label lblMetricaMovimientos;

    @FXML
    private TableView<RegistroCaja> tablaMovimientos;

    @FXML
    private TableColumn<RegistroCaja, String> colHora;

    @FXML
    private TableColumn<RegistroCaja, String> colTipo;

    @FXML
    private TableColumn<RegistroCaja, String> colDescripcion;

    @FXML
    private TableColumn<RegistroCaja, String> colMonto;

    private final ObservableList<RegistroCaja> movimientos = FXCollections.observableArrayList();
    private Caja cajaActual;

    @FXML
    private void initialize() {
        colHora.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().fecha() == null ? "" : cell.getValue().fecha().format(FMT_HORA)));
        colTipo.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().tipo()));
        colDescripcion.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                cell.getValue().descripcion() == null ? "" : cell.getValue().descripcion()));
        colMonto.setCellValueFactory(cell -> new ReadOnlyStringWrapper(
                (cell.getValue().monto().compareTo(BigDecimal.ZERO) >= 0 ? "+ S/ " : "- S/ ")
                        + cell.getValue().monto().abs().toPlainString()));
        colMonto.setCellFactory(col -> new TableCell<RegistroCaja, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("amount-positive", "amount-negative");
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                } else {
                    setText(item);
                    RegistroCaja r = getTableRow().getItem();
                    getStyleClass().add(r.monto().compareTo(BigDecimal.ZERO) >= 0 ? "amount-positive" : "amount-negative");
                }
            }
        });
        tablaMovimientos.setItems(movimientos);
        btnAbrirCaja.setMinWidth(Region.USE_PREF_SIZE);
        btnCerrarCaja.setMinWidth(Region.USE_PREF_SIZE);
        btnRegistrarIngreso.setMinWidth(Region.USE_PREF_SIZE);
        btnRegistrarEgreso.setMinWidth(Region.USE_PREF_SIZE);
        refrescar();
    }

    private void refrescar() {
        try {
            cajaActual = cajaService.getCajaAbierta();
            if (cajaActual != null && !panelEstado.getStyleClass().contains("is-open")) {
                panelEstado.getStyleClass().add("is-open");
            } else if (cajaActual == null) {
                panelEstado.getStyleClass().remove("is-open");
            }
            if (cajaActual == null) {
                lblEstadoCaja.setText("SIN CAJA ABIERTA");
                lblFechaApertura.setText("Abre una caja para comenzar a registrar ventas.");
                lblResumen.setText("S/ 0.00");
                lblMetricaInicial.setText("S/ 0.00");
                lblMetricaVentas.setText("S/ 0.00");
                lblMetricaMovimientos.setText("S/ 0.00");
                movimientos.clear();
            } else {
                lblEstadoCaja.setText("CAJA ABIERTA");
                lblFechaApertura.setText(descripcionApertura(cajaActual.getUsuarioApertura()));
                BigDecimal inicial = cajaActual.getMontoInicial() == null ? BigDecimal.ZERO : cajaActual.getMontoInicial();
                BigDecimal ventas = cajaService.sumarVentas(cajaActual.getIdCaja());

                List<RegistroCaja> lista = new ArrayList<>();
                for (Venta v : cajaService.getVentas(cajaActual.getIdCaja())) {
                    lista.add(new RegistroCaja(v.getFecha(), "Venta", "Venta #" + v.getIdVenta(), v.getTotal()));
                }

                BigDecimal neto = BigDecimal.ZERO;
                for (MovimientoCaja m : cajaService.getMovimientos(cajaActual.getIdCaja())) {
                    BigDecimal monto = m.getMonto() == null ? BigDecimal.ZERO : m.getMonto();
                    if (m.getTipo() == TipoMovimientoCaja.INGRESO) {
                        neto = neto.add(monto);
                        lista.add(new RegistroCaja(m.getFecha(), "Ingreso", m.getDescripcion(), monto));
                    } else {
                        neto = neto.subtract(monto);
                        lista.add(new RegistroCaja(m.getFecha(), "Egreso", m.getDescripcion(), monto.negate()));
                    }
                }
                lista.sort(Comparator.comparing(RegistroCaja::fecha, Comparator.nullsLast(Comparator.naturalOrder())));
                movimientos.setAll(lista);

                lblMetricaInicial.setText("S/ " + inicial.toPlainString());
                lblMetricaVentas.setText("S/ " + ventas.toPlainString());
                lblMetricaMovimientos.setText("S/ " + neto.toPlainString());
                lblResumen.setText("S/ " + inicial.add(ventas).add(neto).toPlainString());
            }
        } catch (Exception e) {
            AlertUtil.mostrarError("Caja", e.getMessage());
        }
    }

    @FXML
    private void onAbrirCaja() {
        if (cajaActual != null) {
            AlertUtil.mostrarAdvertencia("Caja", "Ya existe una caja abierta");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("0.00");
        dialog.setTitle("Abrir caja");
        dialog.setHeaderText(null);
        dialog.setContentText("Monto inicial:");
        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent()) {
            try {
                BigDecimal monto = new BigDecimal(resultado.get());
                cajaService.abrirCaja(monto, SessionManager.getUsuarioActual().getIdUsuario());
                AlertUtil.mostrarInfo("Caja", "Caja abierta correctamente");
                refrescar();
            } catch (NumberFormatException e) {
                AlertUtil.mostrarError("Caja", "Monto inválido");
            } catch (Exception e) {
                AlertUtil.mostrarError("Caja", e.getMessage());
            }
        }
    }

    @FXML
    private void onCerrarCaja() {
        if (cajaActual == null) {
            AlertUtil.mostrarAdvertencia("Caja", "No hay caja abierta");
            return;
        }
        TextInputDialog dialog = new TextInputDialog("0.00");
        dialog.setTitle("Cerrar caja");
        dialog.setHeaderText(null);
        dialog.setContentText("Monto final (declarado):");
        Optional<String> resultado = dialog.showAndWait();
        if (resultado.isPresent()) {
            try {
                BigDecimal montoFinal = new BigDecimal(resultado.get());
                if (montoFinal.compareTo(BigDecimal.ZERO) < 0) {
                    AlertUtil.mostrarAdvertencia("Caja", "El monto final no puede ser negativo");
                    return;
                }
                cajaService.cerrarCaja(montoFinal, SessionManager.getUsuarioActual().getIdUsuario());
                AlertUtil.mostrarInfo("Caja", "Caja cerrada correctamente");
                refrescar();
            } catch (NumberFormatException e) {
                AlertUtil.mostrarError("Caja", "Monto inválido");
            } catch (Exception e) {
                AlertUtil.mostrarError("Caja", e.getMessage());
            }
        }
    }

    @FXML
    private void onRegistrarIngreso() {
        registrarMovimiento(TipoMovimientoCaja.INGRESO);
    }

    @FXML
    private void onRegistrarEgreso() {
        registrarMovimiento(TipoMovimientoCaja.EGRESO);
    }

    private void registrarMovimiento(TipoMovimientoCaja tipo) {
        if (cajaActual == null) {
            AlertUtil.mostrarAdvertencia("Caja", "No hay caja abierta");
            return;
        }
        TextInputDialog montoDialog = new TextInputDialog("0.00");
        montoDialog.setTitle(tipo == TipoMovimientoCaja.INGRESO ? "Registrar ingreso" : "Registrar egreso");
        montoDialog.setHeaderText(null);
        montoDialog.setContentText("Monto:");
        Optional<String> monto = montoDialog.showAndWait();
        if (monto.isEmpty()) {
            return;
        }

        TextInputDialog descDialog = new TextInputDialog();
        descDialog.setTitle("Movimiento");
        descDialog.setHeaderText(null);
        descDialog.setContentText("Descripción:");
        Optional<String> desc = descDialog.showAndWait();

        try {
            MovimientoCaja m = new MovimientoCaja();
            m.setIdCaja(cajaActual.getIdCaja());
            m.setTipo(tipo);
            m.setMonto(new BigDecimal(monto.get()));
            m.setDescripcion(desc.orElse(null));
            m.setIdUsuarioRegistro(SessionManager.getUsuarioActual().getIdUsuario());
            cajaService.registrarMovimiento(m);
            refrescar();
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Caja", "Monto inválido");
        } catch (Exception e) {
            AlertUtil.mostrarError("Caja", e.getMessage());
        }
    }

    private String descripcionApertura(int idUsuario) {
        try {
            Usuario usuario = usuarioService.findById(idUsuario);
            if (usuario != null) {
                return "Turno iniciado por " + usuario.getNombre() + " (" + usuario.getRol() + ")";
            }
        } catch (Exception ignored) {
        }
        return "Turno iniciado por el usuario #" + idUsuario;
    }

    @FXML
    private void onExportarPDF() {
        AlertUtil.mostrarInfo("Exportar", "Generando PDF...");
    }
}
