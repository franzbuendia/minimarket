package com.lacanasta.controller;

import com.lacanasta.model.Categoria;
import com.lacanasta.model.Producto;
import com.lacanasta.service.CategoriaService;
import com.lacanasta.service.ProductoService;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.ProductImageHelper;
import com.lacanasta.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.math.BigDecimal;

/**
 * Controlador del formulario de creación/edición de productos.
 */
public class ProductoFormController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;

    public ProductoFormController(ProductoService productoService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.categoriaService = categoriaService;
    }

    @FXML
    private TextField txtCodigo;

    @FXML
    private TextField txtNombre;

    @FXML
    private ComboBox<Categoria> cmbCategoria;

    @FXML
    private TextField txtPrecioNormal;

    @FXML
    private TextField txtPrecioDescuento;

    @FXML
    private TextField txtCosto;

    @FXML
    private TextField txtStock;

    @FXML
    private TextField txtImagen;

    @FXML
    private StackPane previewContainer;

    @FXML
    private CheckBox chkEstado;

    private Producto producto;

    /**
     * Inicializa el combo de categorías y el listener de preview de imagen.
     */
    @FXML
    private void initialize() {
        cmbCategoria.setItems(FXCollections.observableArrayList(categoriaService.findAll()));
        if (txtImagen != null) {
            txtImagen.textProperty().addListener((obs, oldV, newV) -> actualizarPreview(newV));
            actualizarPreview("");
        }
    }

    private void actualizarPreview(String url) {
        if (previewContainer != null) {
            previewContainer.getChildren().setAll(ProductImageHelper.createThumbnail(url, 38, 38, 6));
        }
    }

    /**
     * Prepara el formulario: vacío para un producto nuevo o cargado para edición.
     *
     * @param producto el producto a editar, o {@code null} para uno nuevo.
     */
    public void initData(Producto producto) {
        this.producto = producto;
        if (producto == null) {
            chkEstado.setSelected(true);
            txtStock.setText("0");
            txtPrecioNormal.setText("0.00");
            txtPrecioDescuento.setText("0.00");
            txtCosto.setText("0.00");
            if (txtImagen != null) {
                txtImagen.setText("");
            }
        } else {
            txtCodigo.setText(producto.getCodigoBarras());
            txtNombre.setText(producto.getNombre());
            txtPrecioNormal.setText(producto.getPrecioNormal() == null ? "0.00" : producto.getPrecioNormal().toString());
            txtPrecioDescuento.setText(producto.getPrecioDescuento() == null ? "0.00" : producto.getPrecioDescuento().toString());
            txtCosto.setText(producto.getCosto() == null ? "0.00" : producto.getCosto().toString());
            txtStock.setText(String.valueOf(producto.getStock()));
            chkEstado.setSelected(producto.isEstado());
            if (txtImagen != null) {
                txtImagen.setText(producto.getImagenUrl() == null ? "" : producto.getImagenUrl());
            }
            if (producto.getIdCategoria() != null) {
                for (Categoria c : cmbCategoria.getItems()) {
                    if (c.getIdCategoria() == producto.getIdCategoria()) {
                        cmbCategoria.getSelectionModel().select(c);
                        break;
                    }
                }
            }
        }
    }

    /**
     * Guarda el producto (crea o actualiza) y cierra el formulario.
     */
    @FXML
    private void onGuardar() {
        try {
            String nombre = txtNombre.getText().trim();
            if (nombre.isEmpty()) {
                AlertUtil.mostrarAdvertencia("Productos", "El nombre es obligatorio");
                return;
            }

            BigDecimal precioNormal = new BigDecimal(txtPrecioNormal.getText().trim());
            BigDecimal precioDescuento = txtPrecioDescuento.getText().trim().isEmpty()
                    ? BigDecimal.ZERO
                    : new BigDecimal(txtPrecioDescuento.getText().trim());
            BigDecimal costo = txtCosto.getText().trim().isEmpty()
                    ? BigDecimal.ZERO
                    : new BigDecimal(txtCosto.getText().trim());
            int stock = Integer.parseInt(txtStock.getText().trim());

            if (stock < 0) {
                AlertUtil.mostrarAdvertencia("Productos", "El stock no puede ser negativo");
                return;
            }

            if (producto == null) {
                producto = new Producto();
            }
            producto.setCodigoBarras(txtCodigo.getText().trim().isEmpty() ? null : txtCodigo.getText().trim());
            producto.setNombre(nombre);
            producto.setPrecioNormal(precioNormal);
            producto.setPrecioDescuento(precioDescuento);
            producto.setCosto(costo);
            producto.setStock(stock);
            producto.setEstado(chkEstado.isSelected());
            if (txtImagen != null) {
                producto.setImagenUrl(txtImagen.getText().trim().isEmpty() ? null : txtImagen.getText().trim());
            }
            Categoria categoria = cmbCategoria.getSelectionModel().getSelectedItem();
            producto.setIdCategoria(categoria == null ? null : categoria.getIdCategoria());
            if (producto.getUsuarioCreacion() == null && SessionManager.getUsuarioActual() != null) {
                producto.setUsuarioCreacion(SessionManager.getUsuarioActual().getIdUsuario());
            }

            productoService.save(producto);
            cerrar();
        } catch (NumberFormatException e) {
            AlertUtil.mostrarError("Error", "Ingrese valores numéricos válidos");
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    @FXML
    private void onSeleccionarImagen() {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(txtNombre.getScene().getWindow());
        modal.setTitle("Galería de imágenes de productos");

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setStyle("-fx-background-color: #f8fafc;");

        Label titulo = new Label("Seleccionar Imagen de Producto");
        titulo.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #113a69;");
        Label subtitulo = new Label("Haz clic sobre una imagen de la biblioteca o examina desde tu equipo.");
        subtitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");

        FlowPane flow = new FlowPane();
        flow.setHgap(12);
        flow.setVgap(12);
        flow.setPrefWrapLength(460);

        for (String imgPath : ProductImageHelper.getPresetImages()) {
            VBox card = new VBox(6);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(8));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-cursor: hand;");

            Node thumb = ProductImageHelper.createThumbnail(imgPath, 52, 52, 8);
            String simpleName = imgPath.substring(imgPath.lastIndexOf('/') + 1)
                    .replace(".png", "")
                    .replace("_", " ");
            Label lbl = new Label(simpleName);
            lbl.setMaxWidth(76);
            lbl.setWrapText(true);
            lbl.setStyle("-fx-font-size: 10px; -fx-text-fill: #475569; -fx-alignment: center;");

            card.getChildren().addAll(thumb, lbl);

            card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #ecfdf5; -fx-background-radius: 10px; -fx-border-color: #10b981; -fx-border-radius: 10px; -fx-cursor: hand;"));
            card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 10px; -fx-border-color: #e2e8f0; -fx-border-radius: 10px; -fx-cursor: hand;"));
            card.setOnMouseClicked(e -> {
                txtImagen.setText(imgPath);
                modal.close();
            });

            flow.getChildren().add(card);
        }

        ScrollPane scroll = new ScrollPane(flow);
        scroll.setFitToWidth(true);
        scroll.setPrefHeight(280);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);

        Button btnExaminar = new Button("Subir desde PC...");
        btnExaminar.setStyle("-fx-background-color: #113a69; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 16;");
        btnExaminar.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Seleccionar foto del producto");
            fc.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("Imágenes (*.png, *.jpg, *.jpeg)", "*.png", "*.jpg", "*.jpeg")
            );
            File file = fc.showOpenDialog(modal);
            if (file != null) {
                txtImagen.setText(file.getAbsolutePath());
                modal.close();
            }
        });

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.setStyle("-fx-background-color: #e2e8f0; -fx-text-fill: #334155; -fx-font-weight: bold; -fx-background-radius: 8px; -fx-padding: 8 16;");
        btnCerrar.setOnAction(e -> modal.close());

        actions.getChildren().addAll(btnExaminar, btnCerrar);

        root.getChildren().addAll(titulo, subtitulo, scroll, actions);

        Scene scene = new Scene(root, 540, 430);
        modal.setScene(scene);
        modal.showAndWait();
    }

    /**
     * Cancela la operación y cierra el formulario.
     */
    @FXML
    private void onCancelar() {
        cerrar();
    }

    private void cerrar() {
        Stage stage = (Stage) txtNombre.getScene().getWindow();
        stage.close();
    }
}
