package com.lacanasta.controller;

import com.lacanasta.dto.ProductoDTO;
import com.lacanasta.model.Categoria;
import com.lacanasta.model.Producto;
import com.lacanasta.service.ProductoGlobalService;
import com.lacanasta.service.ProductoPeruanoService;
import com.lacanasta.service.ProductoService;
import com.lacanasta.service.ServiceException;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.ProductImageHelper;
import com.lacanasta.util.SessionManager;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;

/**
 * Diálogo modal para registrar rápidamente un producto peruano identificado
 * desde el catálogo externo durante el proceso de venta.
 */
public class DialogoProductoNuevo {

    private static final Logger log = LoggerFactory.getLogger(DialogoProductoNuevo.class);

    public enum Accion {
        AGREGAR_Y_VENDER,
        SOLO_AGREGAR,
        CANCELAR
    }

    public static class Resultado {
        private final Accion accion;
        private final Producto producto;

        public Resultado(Accion accion, Producto producto) {
            this.accion = accion;
            this.producto = producto;
        }

        public Accion getAccion() {
            return accion;
        }

        public Producto getProducto() {
            return producto;
        }
    }

    /**
     * Muestra el diálogo modal para la incorporación del producto detectado.
     *
     * @param owner           ventana padre para modalidad.
     * @param dto             datos obtenidos de la API externa o del escaneo.
     * @param categorias      lista de categorías disponibles en el sistema.
     * @param productoService servicio para persistir el producto.
     * @param callback        función invocada cuando el producto es guardado.
     * @return el resultado de la acción seleccionada por el cajero.
     */
    public static Resultado mostrar(Window owner, ProductoDTO dto, List<Categoria> categorias,
                                    ProductoService productoService, Consumer<Producto> callback) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        if (owner != null) {
            modal.initOwner(owner);
        }

        modal.setTitle("Producto Identificado 🌐 - " + dto.getCodigoBarras());
        modal.setResizable(false);

        VBox root = new VBox(16);
        root.setPadding(new Insets(20, 24, 20, 24));
        root.setStyle("-fx-background-color: #f8fafc;");

        // 1. Cabecera con distintivo global
        HBox cabecera = new HBox(12);
        cabecera.setAlignment(Pos.CENTER_LEFT);

        VBox tituloBox = new VBox(2);
        Label lblTitulo = new Label("¡Producto Identificado! 🌐");
        lblTitulo.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #113a69;");
        Label lblSubtitulo = new Label("Compruebe los datos obtenidos del catálogo global y asigne precios y existencias iniciales.");
        lblSubtitulo.setStyle("-fx-font-size: 12px; -fx-text-fill: #64748b;");
        tituloBox.getChildren().addAll(lblTitulo, lblSubtitulo);

        cabecera.getChildren().add(tituloBox);

        // 2. Tarjeta con datos informativos del producto (con imagen destacada a la izquierda 120x120)
        HBox cardInfo = new HBox(16);
        cardInfo.setPadding(new Insets(14));
        cardInfo.setAlignment(Pos.CENTER_LEFT);
        cardInfo.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; "
                + "-fx-border-color: #e2e8f0; -fx-border-radius: 10px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.04), 8, 0, 0, 2);");

        String imgSource = (dto.getImagenRuta() != null && !dto.getImagenRuta().isBlank())
                ? dto.getImagenRuta() : dto.getImagenUrl();
        Node preview = ProductImageHelper.createThumbnail(imgSource, 120, 120, 10);

        GridPane gridInfo = new GridPane();
        gridInfo.setHgap(10);
        gridInfo.setVgap(8);
        HBox.setHgrow(gridInfo, Priority.ALWAYS);

        gridInfo.add(new Label("Código:"), 0, 0);
        Label valCodigo = new Label(dto.getCodigoBarras());
        valCodigo.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-family: monospace;");
        gridInfo.add(valCodigo, 1, 0);

        gridInfo.add(new Label("Nombre:"), 0, 1);
        String nombreInicial = ProductoGlobalService.limpiarNombre(dto.getNombre(), dto.getCodigoBarras());
        TextField txtNombre = new TextField(nombreInicial);
        txtNombre.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        GridPane.setHgrow(txtNombre, Priority.ALWAYS);
        gridInfo.add(txtNombre, 1, 1);

        gridInfo.add(new Label("Marca:"), 0, 2);
        TextField txtMarca = new TextField(dto.getMarca() != null ? dto.getMarca() : "");
        txtMarca.setPromptText("Ej. AlaCena, Coca-Cola, Ferrero");
        GridPane.setHgrow(txtMarca, Priority.ALWAYS);
        gridInfo.add(txtMarca, 1, 2);

        gridInfo.add(new Label("Categoría:"), 0, 3);
        ComboBox<Categoria> cmbCategoria = new ComboBox<>();
        if (categorias != null) {
            cmbCategoria.setItems(FXCollections.observableArrayList(categorias));
            preseleccionarCategoria(cmbCategoria, dto.getCategoria());
        }
        cmbCategoria.setMaxWidth(Double.MAX_VALUE);
        gridInfo.add(cmbCategoria, 1, 3);

        cardInfo.getChildren().addAll(preview, gridInfo);

        // 3. Tarjeta de valores comerciales (precios y stock)
        GridPane gridValores = new GridPane();
        gridValores.setHgap(16);
        gridValores.setVgap(8);
        gridValores.setPadding(new Insets(14));
        gridValores.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 10px; "
                + "-fx-border-color: #cbd5e1; -fx-border-radius: 10px;");

        Label lblPv = new Label("Precio Venta (S/)*:");
        lblPv.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        TextField txtPrecioVenta = new TextField("0.00");
        txtPrecioVenta.setPromptText("Ej. 3.50");
        txtPrecioVenta.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #166534;");

        Label lblPc = new Label("Precio Compra (S/):");
        lblPc.setStyle("-fx-text-fill: #475569;");
        TextField txtPrecioCompra = new TextField("0.00");
        txtPrecioCompra.setPromptText("Ej. 2.50");

        Label lblSt = new Label("Stock Inicial*:");
        lblSt.setStyle("-fx-font-weight: bold; -fx-text-fill: #0f172a;");
        TextField txtStock = new TextField("10");
        txtStock.setPromptText("Ej. 10");

        gridValores.add(lblPv, 0, 0);
        gridValores.add(txtPrecioVenta, 0, 1);
        gridValores.add(lblPc, 1, 0);
        gridValores.add(txtPrecioCompra, 1, 1);
        gridValores.add(lblSt, 2, 0);
        gridValores.add(txtStock, 2, 1);

        // 4. Botonera de acciones
        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER_RIGHT);
        botones.setPadding(new Insets(6, 0, 0, 0));

        Button btnCancelar = new Button("Cancelar");
        btnCancelar.getStyleClass().addAll("btn", "btn-secondary");
        btnCancelar.setGraphic(new FontIcon("fas-times"));

        Button btnSoloAgregar = new Button("Solo agregar");
        btnSoloAgregar.getStyleClass().addAll("btn", "btn-soft");
        btnSoloAgregar.setGraphic(new FontIcon("fas-save"));

        Button btnAgregarVender = new Button("Agregar y vender");
        btnAgregarVender.getStyleClass().addAll("btn", "btn-primary");
        btnAgregarVender.setGraphic(new FontIcon("fas-cart-plus"));
        btnAgregarVender.setDefaultButton(true);

        botones.getChildren().addAll(btnCancelar, btnSoloAgregar, btnAgregarVender);

        root.getChildren().addAll(cabecera, cardInfo, gridValores, botones);

        Scene scene = new Scene(root, 620, 520);
        scene.getStylesheets().add(DialogoProductoNuevo.class.getResource("/css/styles.css").toExternalForm());
        modal.setScene(scene);

        final Resultado[] resultadoFinal = new Resultado[]{new Resultado(Accion.CANCELAR, null)};

        btnCancelar.setOnAction(e -> {
            resultadoFinal[0] = new Resultado(Accion.CANCELAR, null);
            modal.close();
        });

        btnSoloAgregar.setOnAction(e -> {
            Producto guardado = validarYGuardar(dto, txtNombre, txtMarca, txtPrecioVenta, txtPrecioCompra,
                    txtStock, cmbCategoria, productoService);
            if (guardado != null) {
                if (callback != null) {
                    callback.accept(guardado);
                }
                resultadoFinal[0] = new Resultado(Accion.SOLO_AGREGAR, guardado);
                modal.close();
            }
        });

        btnAgregarVender.setOnAction(e -> {
            Producto guardado = validarYGuardar(dto, txtNombre, txtMarca, txtPrecioVenta, txtPrecioCompra,
                    txtStock, cmbCategoria, productoService);
            if (guardado != null) {
                if (callback != null) {
                    callback.accept(guardado);
                }
                resultadoFinal[0] = new Resultado(Accion.AGREGAR_Y_VENDER, guardado);
                modal.close();
            }
        });

        txtPrecioVenta.requestFocus();
        modal.showAndWait();
        return resultadoFinal[0];
    }

    private static void preseleccionarCategoria(ComboBox<Categoria> cmb, String catNombre) {
        if (cmb.getItems().isEmpty()) {
            return;
        }
        if (catNombre != null) {
            String lower = catNombre.toLowerCase();
            for (Categoria c : cmb.getItems()) {
                String cNom = c.getNombre().toLowerCase();
                if (lower.contains(cNom) || cNom.contains(lower)) {
                    cmb.getSelectionModel().select(c);
                    return;
                }
            }
            if (lower.contains("beverage") || lower.contains("drink") || lower.contains("soda") || lower.contains("agua") || lower.contains("juice")) {
                seleccionarPorNombre(cmb, "Bebidas");
                return;
            }
            if (lower.contains("milk") || lower.contains("queso") || lower.contains("yogurt") || lower.contains("dairy") || lower.contains("cheese")) {
                seleccionarPorNombre(cmb, "Lácteos");
                return;
            }
            if (lower.contains("clean") || lower.contains("deterg") || lower.contains("jabon") || lower.contains("soap")) {
                seleccionarPorNombre(cmb, "Limpieza");
                return;
            }
            if (lower.contains("grocery") || lower.contains("snack") || lower.contains("spread") || lower.contains("fideos") || lower.contains("pasta") || lower.contains("arroz")) {
                seleccionarPorNombre(cmb, "Abarrotes");
                return;
            }
            if (lower.contains("import") || lower.contains("international") || lower.contains("foreign")) {
                seleccionarPorNombre(cmb, "Importados");
                return;
            }
        }
        // Categoría por defecto: intentar Abarrotes, luego Importados o primera disponible
        for (Categoria c : cmb.getItems()) {
            if ("Abarrotes".equalsIgnoreCase(c.getNombre())) {
                cmb.getSelectionModel().select(c);
                return;
            }
        }
        for (Categoria c : cmb.getItems()) {
            if ("Importados".equalsIgnoreCase(c.getNombre())) {
                cmb.getSelectionModel().select(c);
                return;
            }
        }
        cmb.getSelectionModel().selectFirst();
    }

    private static void seleccionarPorNombre(ComboBox<Categoria> cmb, String nombre) {
        for (Categoria c : cmb.getItems()) {
            if (c.getNombre().equalsIgnoreCase(nombre)) {
                cmb.getSelectionModel().select(c);
                return;
            }
        }
        cmb.getSelectionModel().selectFirst();
    }

    private static Producto validarYGuardar(ProductoDTO dto, TextField txtNombre, TextField txtMarca,
                                            TextField txtPrecioVenta, TextField txtPrecioCompra,
                                            TextField txtStock, ComboBox<Categoria> cmbCategoria,
                                            ProductoService productoService) {
        String codigo = dto.getCodigoBarras() != null ? dto.getCodigoBarras().trim() : "";
        String nombre = ProductoGlobalService.limpiarNombre(txtNombre.getText(), codigo);
        String marca = txtMarca != null ? txtMarca.getText().trim() : "";

        log.info("[GLOBAL-SCAN] [GUARDAR] Iniciando validación para código '{}' - Nombre: '{}' | Marca: '{}'", codigo, nombre, marca);

        if (nombre.isEmpty()) {
            AlertUtil.mostrarError("Validación", "El nombre del producto no puede estar vacío.");
            txtNombre.requestFocus();
            return null;
        }

        BigDecimal pVenta;
        try {
            pVenta = new BigDecimal(txtPrecioVenta.getText().trim().replace(',', '.'));
            if (pVenta.compareTo(BigDecimal.ZERO) <= 0) {
                AlertUtil.mostrarError("Validación", "El precio de venta debe ser mayor a 0.");
                txtPrecioVenta.requestFocus();
                return null;
            }
        } catch (Exception ex) {
            log.warn("[GLOBAL-SCAN] [GUARDAR] Formato de precio de venta inválido: '{}'", txtPrecioVenta.getText());
            AlertUtil.mostrarError("Validación", "Ingrese un precio de venta numérico válido (ej. 4.50).");
            txtPrecioVenta.requestFocus();
            return null;
        }

        BigDecimal pCompra = BigDecimal.ZERO;
        String txtPc = txtPrecioCompra.getText().trim().replace(',', '.');
        if (!txtPc.isEmpty()) {
            try {
                pCompra = new BigDecimal(txtPc);
                if (pCompra.compareTo(BigDecimal.ZERO) < 0) {
                    AlertUtil.mostrarError("Validación", "El precio de compra no puede ser negativo.");
                    txtPrecioCompra.requestFocus();
                    return null;
                }
            } catch (Exception ex) {
                log.warn("[GLOBAL-SCAN] [GUARDAR] Formato de precio de compra inválido: '{}'", txtPc);
                AlertUtil.mostrarError("Validación", "Ingrese un precio de compra numérico válido.");
                txtPrecioCompra.requestFocus();
                return null;
            }
        }

        int stock;
        try {
            stock = Integer.parseInt(txtStock.getText().trim());
            if (stock < 0) {
                AlertUtil.mostrarError("Validación", "El stock no puede ser negativo.");
                txtStock.requestFocus();
                return null;
            }
        } catch (Exception ex) {
            log.warn("[GLOBAL-SCAN] [GUARDAR] Formato de stock inválido: '{}'", txtStock.getText());
            AlertUtil.mostrarError("Validación", "El stock debe ser un número entero válido (ej. 10).");
            txtStock.requestFocus();
            return null;
        }

        // Selección o inferencia de categoría válida
        Categoria cat = cmbCategoria.getSelectionModel().getSelectedItem();
        Integer idCat = cat != null ? cat.getIdCategoria() : null;
        String catNombre = cat != null ? cat.getNombre() : "Abarrotes";
        if (idCat == null) {
            if (cmbCategoria.getItems() != null && !cmbCategoria.getItems().isEmpty()) {
                Categoria fallback = cmbCategoria.getItems().stream()
                        .filter(c -> "Abarrotes".equalsIgnoreCase(c.getNombre()) || "Varios".equalsIgnoreCase(c.getNombre()))
                        .findFirst()
                        .orElse(cmbCategoria.getItems().get(0));
                idCat = fallback.getIdCategoria();
                catNombre = fallback.getNombre();
            } else {
                idCat = 2; // Respaldo id Abarrotes
                catNombre = "Abarrotes";
            }
        }

        // Determinar usuario auditor (si no hay sesión activa, id=1 Administrador por defecto)
        Integer idUsuario = 1;
        if (SessionManager.getUsuarioActual() != null && SessionManager.getUsuarioActual().getIdUsuario() > 0) {
            idUsuario = SessionManager.getUsuarioActual().getIdUsuario();
        }

        // 1. Verificación previa de duplicados antes de insertar en BD
        try {
            Producto existente = productoService.findByCodigoBarras(codigo);
            if (existente != null) {
                log.warn("[GLOBAL-SCAN] [GUARDAR] Conflicto UNIQUE: El producto con código {} ya existe en el sistema [id={}, nombre='{}']",
                        codigo, existente.getIdProducto(), existente.getNombre());
                AlertUtil.mostrarAdvertencia("Producto ya registrado",
                        "El producto ya está registrado en el sistema:\n\n"
                        + "• Código: " + codigo + "\n"
                        + "• Nombre: " + existente.getNombre() + "\n"
                        + "• Marca: " + (existente.getMarca() != null ? existente.getMarca() : "(No registrada)") + "\n"
                        + "• Stock actual: " + existente.getStock() + " un.\n"
                        + "• Precio: S/ " + existente.getPrecioNormal());
                return existente;
            }
        } catch (Exception e) {
            log.warn("[GLOBAL-SCAN] [GUARDAR] No se pudo verificar existencia previa: {}", e.getMessage());
        }

        // Descarga de imagen si aún no se tiene en local
        String rutaLocal = dto.getImagenRuta();
        String imgUrl = dto.getImagenUrl();
        if ((rutaLocal == null || rutaLocal.isBlank()) && imgUrl != null && !imgUrl.isBlank()) {
            try {
                rutaLocal = new ProductoGlobalService().descargarImagenLocal(imgUrl, codigo);
                dto.setImagenRuta(rutaLocal);
            } catch (Exception ignored) {
            }
        }

        // 2. Construcción de entidad con todos los campos obligatorios
        Producto nuevo = new Producto();
        nuevo.setCodigoBarras(codigo);
        nuevo.setNombre(nombre);
        nuevo.setMarca(marca);
        nuevo.setPrecioNormal(pVenta);
        nuevo.setPrecioDescuento(BigDecimal.ZERO);
        nuevo.setCosto(pCompra);
        nuevo.setStock(stock);
        nuevo.setEstado(true);
        nuevo.setIdCategoria(idCat);
        nuevo.setCategoriaNombre(catNombre);

        if (imgUrl != null && imgUrl.length() > 255) {
            imgUrl = imgUrl.substring(0, 255);
        }
        nuevo.setImagenUrl(imgUrl);
        nuevo.setImagenRuta(rutaLocal);
        nuevo.setUsuarioCreacion(idUsuario);
        nuevo.setFechaCreacion(LocalDateTime.now());

        log.info("[GLOBAL-SCAN] [GUARDAR] Enviando a persistencia: código={}, nombre='{}', marca='{}', precio={}, costo={}, stock={}, idCat={}, rutaFoto='{}'",
                codigo, nombre, marca, pVenta, pCompra, stock, idCat, rutaLocal);

        try {
            productoService.save(nuevo);
            log.info("[GLOBAL-SCAN] [GUARDAR] ¡Guardado exitoso en MySQL! ID asignado: {}", nuevo.getIdProducto());
            Producto creado = productoService.findByCodigoBarras(codigo);
            return creado != null ? creado : nuevo;
        } catch (ServiceException ex) {
            log.error("[GLOBAL-SCAN] [GUARDAR] Error de servicio al guardar producto [código: {}]: {}", codigo, ex.getMessage(), ex);
            String detalle = ex.getMessage();
            if (ex.getCause() != null && ex.getCause().getMessage() != null && !ex.getCause().getMessage().isBlank()) {
                detalle += "\n(" + ex.getCause().getMessage() + ")";
            }
            AlertUtil.mostrarError("Error al guardar", "No se pudo guardar el producto:\n" + detalle);
            return null;
        } catch (Exception ex) {
            log.error("[GLOBAL-SCAN] [GUARDAR] Error inesperado al guardar producto [código: {}]: {}", codigo, ex.getMessage(), ex);
            String detalle = ex.getMessage();
            if (ex.getCause() != null && ex.getCause().getMessage() != null) {
                detalle += "\n(" + ex.getCause().getMessage() + ")";
            }
            AlertUtil.mostrarError("Error al guardar", "Error inesperado al guardar el producto:\n" + detalle);
            return null;
        }
    }
}
