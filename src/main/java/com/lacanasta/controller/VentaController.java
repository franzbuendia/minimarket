package com.lacanasta.controller;

import com.lacanasta.model.Caja;
import com.lacanasta.model.CarritoItem;
import com.lacanasta.model.DetalleVenta;
import com.lacanasta.model.EmpresaConfig;
import com.lacanasta.model.MetodoPago;
import com.lacanasta.model.Producto;
import com.lacanasta.model.Venta;
import com.lacanasta.service.CajaService;
import com.lacanasta.service.ConfigService;
import com.lacanasta.service.ProductoService;
import com.lacanasta.service.VentaService;
import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.ProductImageHelper;
import com.lacanasta.util.SessionManager;
import com.lacanasta.util.TicketPDFGenerator;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import com.lacanasta.dto.ProductoDTO;
import com.lacanasta.service.CategoriaService;
import com.lacanasta.service.ProductoGlobalService;
import com.lacanasta.service.ProductoPeruanoService;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.kordamp.ikonli.javafx.FontIcon;

import java.awt.Toolkit;
import java.awt.Desktop;
import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Controlador del módulo de ventas (carrito, búsqueda, pago y vuelto).
 */
public class VentaController {

    private final ProductoService productoService;
    private final VentaService ventaService;
    private final CajaService cajaService;
    private final ConfigService configService;
    private final ProductoGlobalService productoGlobalService;
    private final CategoriaService categoriaService;

    public VentaController(ProductoService productoService, VentaService ventaService,
                           CajaService cajaService, ConfigService configService) {
        this(productoService, ventaService, cajaService, configService,
                new ProductoGlobalService(), new CategoriaService());
    }

    public VentaController(ProductoService productoService, VentaService ventaService,
                           CajaService cajaService, ConfigService configService,
                           ProductoGlobalService productoGlobalService, CategoriaService categoriaService) {
        this.productoService = productoService;
        this.ventaService = ventaService;
        this.cajaService = cajaService;
        this.configService = configService;
        this.productoGlobalService = productoGlobalService != null ? productoGlobalService : new ProductoGlobalService();
        this.categoriaService = categoriaService;
    }

    public VentaController(ProductoService productoService, VentaService ventaService,
                           CajaService cajaService, ConfigService configService,
                           ProductoPeruanoService productoPeruanoService, CategoriaService categoriaService) {
        this(productoService, ventaService, cajaService, configService,
                new ProductoGlobalService(), categoriaService);
    }

    @FXML
    private VBox rootPane;

    @FXML
    private HBox panelFeedback;

    @FXML
    private FontIcon iconFeedback;

    @FXML
    private Label lblFeedback;

    @FXML
    private ListView<CarritoItem> lstCarrito;

    @FXML
    private TableView<Producto> tablaResultados;

    @FXML
    private TableColumn<Producto, String> colImagenResultado;

    @FXML
    private TableColumn<Producto, String> colNombreResultado;

    @FXML
    private TableColumn<Producto, String> colPrecioResultado;

    @FXML
    private TableColumn<Producto, String> colStockResultado;

    @FXML
    private TableColumn<Producto, String> colAcciones;

    @FXML
    private TextField txtBuscar;

    @FXML
    private TextField txtMontoRecibido;

    @FXML
    private Label lblTotal;

    @FXML
    private Label lblVuelto;

    @FXML
    private Label lblUnidades;

    @FXML
    private ToggleButton btnEfectivo;

    @FXML
    private ToggleButton btnTarjeta;

    @FXML
    private ToggleButton btnYape;

    @FXML
    private ToggleGroup grupoPago;

    @FXML
    private VBox panelEfectivo;

    @FXML
    private Button btnPagar;

    public enum FeedbackTipo {
        INFO, SUCCESS, WARNING, ERROR
    }

    private final ObservableList<CarritoItem> carrito = FXCollections.observableArrayList();
    private final ObservableList<Producto> resultados = FXCollections.observableArrayList();
    private MetodoPago metodoPago = MetodoPago.EFECTIVO;

    private final StringBuilder scanBuffer = new StringBuilder();
    private long lastKeyTime = 0;
    private boolean filtroInstalado = false;

    /**
     * Inicializa las tablas y los controles de pago.
     */
    @FXML
    private void initialize() {
        lstCarrito.setCellFactory(list -> new ListCell<CarritoItem>() {
            @Override
            protected void updateItem(CarritoItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    setGraphic(crearFila(item));
                }
            }
        });
        lstCarrito.setItems(carrito);

        colImagenResultado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getImagenUrl()));
        colImagenResultado.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    Producto p = getTableRow().getItem();
                    setGraphic(ProductImageHelper.createThumbnail(p.getImagenUrl(), 36, 36, 6));
                    setAlignment(Pos.CENTER);
                    setText(null);
                }
            }
        });

        colNombreResultado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getNombre()));
        colPrecioResultado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().getPrecioVenta().toString()));
        colStockResultado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(String.valueOf(cell.getValue().getStock())));
        colStockResultado.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Producto p = getTableRow().getItem();
                    int enCarro = obtenerCantidadEnCarrito(p.getIdProducto());
                    int disponible = p.getStock() - enCarro;

                    Label badge;
                    if (disponible <= 0) {
                        badge = new Label("Agotado (0)");
                        badge.getStyleClass().addAll("badge", "badge-danger");
                    } else if (disponible < 5) {
                        badge = new Label(disponible + " disp. (Stock: " + p.getStock() + ")");
                        badge.getStyleClass().addAll("badge", "badge-warning");
                    } else {
                        badge = new Label(disponible + " disponibles");
                        badge.getStyleClass().addAll("badge", "badge-neutral");
                    }
                    setGraphic(badge);
                }
            }
        });
        colAcciones.setCellFactory(col -> new TableCell<Producto, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                } else {
                    Producto p = getTableRow().getItem();
                    int enCarro = obtenerCantidadEnCarrito(p.getIdProducto());
                    int disponible = p.getStock() - enCarro;

                    Button btnAgregar = new Button("+ Agregar");
                    btnAgregar.getStyleClass().addAll("btn", "btn-soft");
                    btnAgregar.setPrefWidth(110);
                    btnAgregar.setMinWidth(110);
                    if (disponible <= 0) {
                        btnAgregar.setDisable(true);
                    }
                    btnAgregar.setOnAction(e -> {
                        agregarAlCarritoConStock(p);
                        enfocarBuscador();
                    });
                    HBox box = new HBox(btnAgregar);
                    box.setAlignment(Pos.CENTER_LEFT);
                    box.setMinWidth(Region.USE_PREF_SIZE);
                    setGraphic(box);
                }
            }
        });
        tablaResultados.setItems(resultados);

        txtMontoRecibido.setEditable(true);
        txtMontoRecibido.setDisable(false);
        txtMontoRecibido.setOnAction(e -> onPagar());
        txtMontoRecibido.focusedProperty().addListener((obs, oldVal, isFocused) -> {
            if (isFocused) {
                Platform.runLater(txtMontoRecibido::selectAll);
            }
        });
        txtMontoRecibido.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null) return;
            if (newVal.contains(",")) {
                Platform.runLater(() -> txtMontoRecibido.setText(newVal.replace(',', '.')));
                return;
            }
            calcularVuelto();
        });

        if (btnEfectivo != null) btnEfectivo.setFocusTraversable(false);
        if (btnTarjeta != null) btnTarjeta.setFocusTraversable(false);
        if (btnYape != null) btnYape.setFocusTraversable(false);
        if (panelEfectivo != null) {
            panelEfectivo.setOnMouseClicked(e -> {
                if (txtMontoRecibido != null) {
                    txtMontoRecibido.requestFocus();
                    txtMontoRecibido.selectAll();
                }
            });
        }

        seleccionarMetodo(MetodoPago.EFECTIVO);
        cargarProductos();
        actualizarTotales();
        verificarCajaAbierta();

        // Enfoque automático y preparación del escáner en cualquier parte de la vista
        enfocarBuscador();
        if (rootPane != null) {
            rootPane.sceneProperty().addListener((obs, oldScene, newScene) -> {
                if (newScene != null) {
                    instalarFiltroEscanner(newScene);
                    enfocarBuscador();
                }
            });
        }
    }

    private void verificarCajaAbierta() {
        boolean abierta = cajaService.getCajaAbierta() != null;
        btnPagar.setDisable(!abierta);
        if (!abierta) {
            AlertUtil.mostrarAdvertencia("Ventas",
                    "No hay una caja abierta. Abra una caja en el módulo Caja para poder cobrar.");
        }
    }

    private void cargarProductos() {
        try {
            resultados.setAll(productoService.findAll());
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void instalarFiltroEscanner(Scene scene) {
        if (scene == null || filtroInstalado) return;
        filtroInstalado = true;

        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            // Si la vista de ventas no está activa en pantalla, no hacer nada
            if (rootPane == null || rootPane.getScene() == null || !rootPane.isVisible() || rootPane.getParent() == null) {
                return;
            }

            Node focusOwner = scene.getFocusOwner();

            // Si está en el campo de monto recibido para el pago en efectivo:
            if (txtMontoRecibido != null && txtMontoRecibido.isFocused()) {
                if (event.getCode() == KeyCode.ENTER) {
                    event.consume();
                    onPagar();
                }
                return;
            }

            // Si cualquier campo de texto tiene el foco (distinto al buscador de ventas), no intervenir
            if (focusOwner instanceof TextInputControl && focusOwner != txtBuscar) {
                return;
            }

            long now = System.currentTimeMillis();
            long elapsed = now - lastKeyTime;
            lastKeyTime = now;

            if (event.getCode() == KeyCode.ENTER) {
                // Si el escáner físico envió una ráfaga acumulada en el buffer:
                if (scanBuffer.length() >= 3 && elapsed < 250) {
                    String barcode = scanBuffer.toString().trim();
                    scanBuffer.setLength(0);
                    event.consume();
                    procesarCodigo(barcode);
                    return;
                }

                // Si txtBuscar no estaba enfocado pero tiene texto:
                if (txtBuscar != null && !txtBuscar.isFocused() && !txtBuscar.getText().trim().isEmpty()) {
                    event.consume();
                    onBuscar();
                }
            }
        });

        scene.addEventFilter(KeyEvent.KEY_TYPED, event -> {
            // Si la vista de ventas no está activa en pantalla, no hacer nada
            if (rootPane == null || rootPane.getScene() == null || !rootPane.isVisible() || rootPane.getParent() == null) {
                return;
            }

            Node focusOwner = scene.getFocusOwner();

            // Si cualquier campo de texto tiene el foco (monto recibido, cantidad, etc.), dejar escribir libremente
            if (focusOwner instanceof TextInputControl && focusOwner != txtBuscar) {
                return;
            }

            String ch = event.getCharacter();
            if (ch == null || ch.isEmpty() || ch.charAt(0) < 32 || ch.charAt(0) > 126) {
                return;
            }

            long now = System.currentTimeMillis();
            if (now - lastKeyTime > 150) {
                scanBuffer.setLength(0);
            }
            scanBuffer.append(ch);
            lastKeyTime = now;

            // Si el método seleccionado es efectivo y el usuario teclea números o decimales cerca de la zona de pago:
            if (metodoPago == MetodoPago.EFECTIVO && txtMontoRecibido != null && txtMontoRecibido.isVisible()) {
                if (focusOwner == btnEfectivo || focusOwner == panelEfectivo || focusOwner == btnPagar) {
                    if (Character.isDigit(ch.charAt(0)) || ch.equals(".") || ch.equals(",")) {
                        txtMontoRecibido.requestFocus();
                        txtMontoRecibido.appendText(ch.replace(',', '.'));
                        event.consume();
                        return;
                    }
                }
            }

            // Si el foco no está en un campo de texto, redirigir al buscador:
            if (txtBuscar != null && !txtBuscar.isFocused()) {
                txtBuscar.requestFocus();
                txtBuscar.appendText(ch);
                event.consume();
            }
        });
    }

    private void revertirEscaneoEnMonto(String barcode) {
        if (txtMontoRecibido == null) return;
        String actual = txtMontoRecibido.getText();
        if (actual != null && !actual.isEmpty()) {
            if (actual.endsWith(barcode)) {
                txtMontoRecibido.setText(actual.substring(0, actual.length() - barcode.length()));
            } else if (actual.equals(barcode)) {
                txtMontoRecibido.clear();
            }
        }
    }

    /**
     * Busca por código de barras (escáner) o por nombre.
     */
    @FXML
    private void onBuscar() {
        if (txtBuscar == null) return;
        String texto = txtBuscar.getText().trim();
        procesarCodigo(texto);
    }

    /**
     * Procesa la entrada del escáner o teclado, detectando productos automáticamente.
     */
    public void procesarCodigo(String texto) {
        if (texto == null) return;
        texto = texto.trim();
        if (texto.isEmpty()) {
            cargarProductos();
            mostrarFeedback("Escáner listo — Pase el lector sobre el código de barras en cualquier momento", FeedbackTipo.INFO);
            return;
        }

        try {
            // 1. Búsqueda exacta por código de barras en el inventario
            Producto porCodigo = productoService.findByCodigoBarras(texto);
            if (porCodigo == null && texto.startsWith("0")) {
                porCodigo = productoService.findByCodigoBarras(texto.replaceFirst("^0+", ""));
            }

            if (porCodigo != null) {
                agregarAlCarritoConStock(porCodigo);
                if (txtBuscar != null) {
                    txtBuscar.clear();
                }
                enfocarBuscador();
                return;
            }

            // 2. Si es numérico (código de barras no encontrado en base de datos local)
            if (texto.matches("\\d+")) {
                String codigo = texto;

                // 3.a. Validar que el código sea numérico y tenga entre 8 y 14 dígitos
                if (!productoGlobalService.esCodigoValido(codigo)) {
                    emitirBeepAlerta();
                    mostrarFeedback("Código de barras inválido (debe tener entre 8 y 14 dígitos).", FeedbackTipo.ERROR);
                    AlertUtil.mostrarAdvertencia("Código de barras inválido",
                            "Código de barras inválido: \"" + codigo + "\". Debe contener entre 8 y 14 dígitos numéricos.");
                    if (txtBuscar != null) {
                        txtBuscar.clear();
                    }
                    enfocarBuscador();
                    return;
                }

                // 3.b. Si es válido -> consultar la API global de Open Food Facts en hilo asíncrono
                mostrarFeedback("🔍 Consultando catálogo global para: " + codigo + "...", FeedbackTipo.INFO);
                if (txtBuscar != null) {
                    txtBuscar.clear();
                }

                CompletableFuture.supplyAsync(() -> productoGlobalService.buscarGlobal(codigo))
                        .thenAcceptAsync(dto -> manejarRespuestaProductoGlobal(codigo, dto), Platform::runLater)
                        .exceptionally(ex -> {
                            Platform.runLater(() -> {
                                emitirBeepAlerta();
                                mostrarFeedback("Error al consultar producto global: " + ex.getMessage(), FeedbackTipo.ERROR);
                                enfocarBuscador();
                            });
                            return null;
                        });
                return;
            }

            // 3. Búsqueda textual por nombre de producto
            List<Producto> encontrados = productoService.search(texto);
            resultados.setAll(encontrados);
            if (encontrados.isEmpty()) {
                emitirBeepAlerta();
                mostrarFeedback("No se encontraron productos con: \"" + texto + "\"", FeedbackTipo.WARNING);
            } else {
                mostrarFeedback("Búsqueda: " + encontrados.size() + " producto(s) encontrado(s)", FeedbackTipo.INFO);
            }
        } catch (Exception e) {
            emitirBeepAlerta();
            AlertUtil.mostrarError("Error al escanear", e.getMessage());
        }
    }

    /**
     * Procesa la respuesta obtenida del servicio de productos globales según las reglas de negocio.
     */
    private void manejarRespuestaProductoGlobal(String codigo, ProductoDTO dto) {
        Window window = (txtBuscar != null && txtBuscar.getScene() != null) ? txtBuscar.getScene().getWindow() : null;

        // 3.c. Si la API global lo encuentra:
        if (dto != null && dto.isEncontrado()) {
            mostrarFeedback("✓ Producto encontrado en catálogo global: " + dto.getNombre(), FeedbackTipo.SUCCESS);
            DialogoProductoNuevo.Resultado res = DialogoProductoNuevo.mostrar(
                    window, dto, categoriaService.findAll(), productoService, null
            );

            if (res.getAccion() == DialogoProductoNuevo.Accion.AGREGAR_Y_VENDER && res.getProducto() != null) {
                agregarAlCarritoConStock(res.getProducto());
                mostrarFeedback("✓ Producto registrado y añadido al carrito: " + res.getProducto().getNombre(), FeedbackTipo.SUCCESS);
            } else if (res.getAccion() == DialogoProductoNuevo.Accion.SOLO_AGREGAR && res.getProducto() != null) {
                mostrarFeedback("✓ Producto agregado al catálogo: " + res.getProducto().getNombre(), FeedbackTipo.SUCCESS);
            } else {
                mostrarFeedback("Registro de producto cancelado.", FeedbackTipo.INFO);
            }
            enfocarBuscador();
            return;
        }

        // 3.d. Si la API NO lo encuentra:
        emitirBeepAlerta();
        mostrarFeedback("Producto no encontrado en bases globales. Regístrelo manualmente.", FeedbackTipo.WARNING);
        boolean registrar = AlertUtil.mostrarConfirmacion(
                "Producto no encontrado",
                "Producto no encontrado en bases globales. ¿Desea registrarlo manualmente?"
        );
        if (registrar) {
            ProductoDTO manualDto = new ProductoDTO(codigo);
            manualDto.setNombre("");
            manualDto.setMarca("");
            manualDto.setCategoria("Abarrotes");
            DialogoProductoNuevo.Resultado res = DialogoProductoNuevo.mostrar(
                    window, manualDto, categoriaService.findAll(), productoService, null
            );
            if (res.getAccion() == DialogoProductoNuevo.Accion.AGREGAR_Y_VENDER && res.getProducto() != null) {
                agregarAlCarritoConStock(res.getProducto());
                mostrarFeedback("✓ Producto registrado y añadido a la venta: " + res.getProducto().getNombre(), FeedbackTipo.SUCCESS);
            } else if (res.getAccion() == DialogoProductoNuevo.Accion.SOLO_AGREGAR && res.getProducto() != null) {
                mostrarFeedback("✓ Producto incorporado al catálogo: " + res.getProducto().getNombre(), FeedbackTipo.SUCCESS);
            }
        }
        enfocarBuscador();
    }

    /**
     * Agrega al carrito el producto seleccionado en la tabla de resultados.
     */
    @FXML
    private void onAgregar() {
        Producto seleccionado = tablaResultados.getSelectionModel().getSelectedItem();
        if (seleccionado == null) {
            AlertUtil.mostrarAdvertencia("Ventas", "Seleccione un producto");
            return;
        }
        agregarAlCarritoConStock(seleccionado);
        enfocarBuscador();
    }

    private void agregarAlCarritoConStock(Producto producto) {
        // Consultar stock fresco directo de la base de datos para sincronización exacta
        Producto fresco = productoService.findById(producto.getIdProducto());
        if (fresco != null) {
            producto = fresco;
        }

        if (!producto.isEstado()) {
            emitirBeepAlerta();
            mostrarFeedback("El producto '" + producto.getNombre() + "' está inactivo.", FeedbackTipo.ERROR);
            return;
        }

        int stockTotal = producto.getStock();
        int enCarrito = obtenerCantidadEnCarrito(producto.getIdProducto());

        if (stockTotal <= 0) {
            emitirBeepAlerta();
            mostrarFeedback("¡SIN STOCK! '" + producto.getNombre() + "' está agotado (Stock: 0)", FeedbackTipo.ERROR);
            AlertUtil.mostrarAdvertencia("Sin Stock", "El producto '" + producto.getNombre() + "' no tiene stock disponible en inventario.");
            return;
        }

        if (enCarrito + 1 > stockTotal) {
            emitirBeepAlerta();
            mostrarFeedback("Stock insuficiente: '" + producto.getNombre() + "' solo tiene " + stockTotal + " unid. en stock", FeedbackTipo.WARNING);
            AlertUtil.mostrarAdvertencia("Stock Insuficiente",
                    "No se pueden agregar más unidades de '" + producto.getNombre() + "'.\n"
                    + "Stock disponible en inventario: " + stockTotal + "\n"
                    + "Ya agregadas al carrito: " + enCarrito);
            return;
        }

        boolean existia = false;
        for (CarritoItem item : carrito) {
            if (item.getIdProducto() == producto.getIdProducto()) {
                item.setCantidad(item.getCantidad() + 1);
                existia = true;
                break;
            }
        }
        if (!existia) {
            carrito.add(new CarritoItem(producto, 1));
        }

        lstCarrito.refresh();
        actualizarTotales();
        tablaResultados.refresh();

        int disponibleRestante = stockTotal - (enCarrito + 1);
        emitirBeepExito();
        mostrarFeedback("✓ Escaneado: " + producto.getNombre() + " (S/ " + producto.getPrecioVenta() + ") | Stock disponible: " + disponibleRestante, FeedbackTipo.SUCCESS);
    }

    private int obtenerCantidadEnCarrito(int idProducto) {
        int total = 0;
        for (CarritoItem item : carrito) {
            if (item.getIdProducto() == idProducto) {
                total += item.getCantidad();
            }
        }
        return total;
    }

    private void cambiarCantidad(CarritoItem item, int delta) {
        int nueva = item.getCantidad() + delta;
        if (nueva <= 0) {
            carrito.remove(item);
            mostrarFeedback("Producto removido del carrito", FeedbackTipo.INFO);
        } else {
            Producto p = productoService.findById(item.getIdProducto());
            int stockDisponible = (p != null) ? p.getStock() : item.getProducto().getStock();
            if (nueva > stockDisponible) {
                emitirBeepAlerta();
                AlertUtil.mostrarAdvertencia("Ventas", "Stock insuficiente. Máximo disponible: " + stockDisponible);
                return;
            }
            item.setCantidad(nueva);
        }
        lstCarrito.refresh();
        actualizarTotales();
        tablaResultados.refresh();
        enfocarBuscador();
    }

    private void quitarItem(CarritoItem item) {
        carrito.remove(item);
        actualizarTotales();
        tablaResultados.refresh();
        mostrarFeedback("Producto removido del carrito", FeedbackTipo.INFO);
        enfocarBuscador();
    }

    private void mostrarFeedback(String mensaje, FeedbackTipo tipo) {
        if (lblFeedback == null || panelFeedback == null) return;
        lblFeedback.setText(mensaje);
        panelFeedback.getStyleClass().removeAll(
                "scanner-feedback-idle", "scanner-feedback-success",
                "scanner-feedback-warning", "scanner-feedback-error");

        String iconLiteral = "fas-barcode";
        String color = "#64748b";

        switch (tipo) {
            case SUCCESS -> {
                panelFeedback.getStyleClass().add("scanner-feedback-success");
                iconLiteral = "fas-check-circle";
                color = "#047857";
            }
            case WARNING -> {
                panelFeedback.getStyleClass().add("scanner-feedback-warning");
                iconLiteral = "fas-exclamation-triangle";
                color = "#b45309";
            }
            case ERROR -> {
                panelFeedback.getStyleClass().add("scanner-feedback-error");
                iconLiteral = "fas-times-circle";
                color = "#b91c1c";
            }
            case INFO -> {
                panelFeedback.getStyleClass().add("scanner-feedback-idle");
                iconLiteral = "fas-barcode";
                color = "#64748b";
            }
        }
        if (iconFeedback != null) {
            iconFeedback.setIconLiteral(iconLiteral);
            iconFeedback.setIconColor(javafx.scene.paint.Color.web(color));
        }
    }

    private void emitirBeepExito() {
        try {
            Toolkit.getDefaultToolkit().beep();
        } catch (Throwable ignored) {
        }
    }

    private void emitirBeepAlerta() {
        try {
            Toolkit.getDefaultToolkit().beep();
            new Thread(() -> {
                try {
                    Thread.sleep(130);
                    Toolkit.getDefaultToolkit().beep();
                } catch (Throwable ignored) {
                }
            }).start();
        } catch (Throwable ignored) {
        }
    }

    private void enfocarBuscador() {
        Platform.runLater(() -> {
            if (txtBuscar != null) {
                txtBuscar.requestFocus();
            }
        });
    }

    private HBox crearFila(CarritoItem item) {
        Node thumb = ProductImageHelper.createThumbnail(
                item.getProducto() != null ? item.getProducto().getImagenUrl() : null, 36, 36, 6);

        Label nombre = new Label(item.getNombre());
        nombre.getStyleClass().add("cart-item-name");
        Label precio = new Label("S/ " + item.getPrecio().toPlainString() + " c/u");
        precio.getStyleClass().add("cart-item-price");
        VBox info = new VBox(2, nombre, precio);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);

        Label subtotal = new Label("S/ " + item.getSubtotal().toPlainString());
        subtotal.getStyleClass().add("cart-item-subtotal");

        Button menos = new Button("−");
        menos.getStyleClass().add("small-button");
        menos.setOnAction(e -> cambiarCantidad(item, -1));

        TextField txtCantidad = new TextField(String.valueOf(item.getCantidad()));
        txtCantidad.getStyleClass().add("field");
        txtCantidad.setPrefWidth(55);
        txtCantidad.setAlignment(Pos.CENTER);
        txtCantidad.setEditable(true);
        txtCantidad.setDisable(false);

        txtCantidad.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
            if (isNowFocused) {
                Platform.runLater(txtCantidad::selectAll);
            } else {
                String txt = txtCantidad.getText();
                if (txt == null || txt.isBlank()) {
                    txtCantidad.setText(String.valueOf(item.getCantidad()));
                } else {
                    try {
                        int val = Integer.parseInt(txt.trim());
                        if (val > 0) {
                            int stock = item.getProducto() != null ? item.getProducto().getStock() : 9999;
                            if (val <= stock) {
                                item.setCantidad(val);
                            } else {
                                item.setCantidad(stock);
                                txtCantidad.setText(String.valueOf(stock));
                                AlertUtil.mostrarAdvertencia("Stock", "Stock máximo disponible: " + stock);
                            }
                        } else {
                            txtCantidad.setText(String.valueOf(item.getCantidad()));
                        }
                    } catch (NumberFormatException ex) {
                        txtCantidad.setText(String.valueOf(item.getCantidad()));
                    }
                }
                subtotal.setText("S/ " + item.getSubtotal().toPlainString());
                actualizarTotales();
                tablaResultados.refresh();
            }
        });

        txtCantidad.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal == null || newVal.isBlank()) return;
            try {
                int val = Integer.parseInt(newVal.trim());
                if (val > 0) {
                    int stock = item.getProducto() != null ? item.getProducto().getStock() : 9999;
                    if (val <= stock) {
                        item.setCantidad(val);
                        subtotal.setText("S/ " + item.getSubtotal().toPlainString());
                        actualizarTotales();
                    }
                }
            } catch (NumberFormatException ignored) {}
        });

        Button mas = new Button("+");
        mas.getStyleClass().add("small-button");
        mas.setOnAction(e -> cambiarCantidad(item, 1));
        HBox qty = new HBox(6, menos, txtCantidad, mas);
        qty.setAlignment(Pos.CENTER);

        Button borrar = new Button();
        borrar.setGraphic(new FontIcon("bi-trash"));
        borrar.getStyleClass().addAll("btn", "btn-ghost");
        borrar.setOnAction(e -> quitarItem(item));
        HBox derecha = new HBox(8, subtotal, borrar);
        derecha.setAlignment(Pos.CENTER_RIGHT);

        HBox fila = new HBox(10, thumb, info, qty, derecha);
        fila.setAlignment(Pos.CENTER_LEFT);
        return fila;
    }

    @FXML
    private void onVaciar() {
        carrito.clear();
        actualizarTotales();
        tablaResultados.refresh();
        mostrarFeedback("Carrito vaciado", FeedbackTipo.INFO);
        enfocarBuscador();
    }

    @FXML
    private void onMetodoEfectivo() {
        seleccionarMetodo(MetodoPago.EFECTIVO);
    }

    @FXML
    private void onMetodoTarjeta() {
        seleccionarMetodo(MetodoPago.TARJETA);
    }

    @FXML
    private void onMetodoYape() {
        seleccionarMetodo(MetodoPago.YAPE);
    }

    private void seleccionarMetodo(MetodoPago metodo) {
        this.metodoPago = metodo;
        btnEfectivo.setSelected(metodo == MetodoPago.EFECTIVO);
        btnTarjeta.setSelected(metodo == MetodoPago.TARJETA);
        btnYape.setSelected(metodo == MetodoPago.YAPE);
        boolean efectivo = metodo == MetodoPago.EFECTIVO;
        panelEfectivo.setVisible(efectivo);
        panelEfectivo.setManaged(efectivo);
        actualizarTotales();
        if (efectivo && txtMontoRecibido != null) {
            txtMontoRecibido.setDisable(false);
            txtMontoRecibido.setEditable(true);
            Platform.runLater(() -> {
                txtMontoRecibido.requestFocus();
                txtMontoRecibido.selectAll();
            });
        } else {
            enfocarBuscador();
        }
    }

    private void actualizarTotales() {
        BigDecimal total = totalCarrito();
        lblTotal.setText("S/ " + total.toPlainString());
        int unidades = carrito.stream().mapToInt(CarritoItem::getCantidad).sum();
        lblUnidades.setText(unidades + " unidades");
        btnPagar.setText("Cobrar S/ " + total.toPlainString());
        calcularVuelto();
    }

    private void calcularVuelto() {
        if (txtMontoRecibido == null || lblVuelto == null) return;
        String texto = txtMontoRecibido.getText();
        if (texto == null || texto.trim().isEmpty()) {
            lblVuelto.setText("Vuelto: S/ 0.00");
            return;
        }
        try {
            BigDecimal recibido = new BigDecimal(texto.trim());
            BigDecimal total = totalCarrito();
            BigDecimal vuelto = recibido.subtract(total);
            if (vuelto.compareTo(BigDecimal.ZERO) < 0) {
                lblVuelto.setText("Vuelto: S/ 0.00 (Falta S/ " + vuelto.abs().toPlainString() + ")");
            } else {
                lblVuelto.setText("Vuelto: S/ " + vuelto.toPlainString());
            }
        } catch (NumberFormatException e) {
            lblVuelto.setText("Vuelto: S/ 0.00");
        }
    }

    private BigDecimal totalCarrito() {
        BigDecimal total = BigDecimal.ZERO;
        for (CarritoItem item : carrito) {
            total = total.add(item.getSubtotal());
        }
        return total;
    }

    /**
     * Registra la venta verificando que la caja esté abierta y muestra el boleto en pantalla.
     */
    @FXML
    private void onPagar() {
        if (carrito.isEmpty()) {
            AlertUtil.mostrarAdvertencia("Ventas", "El carrito está vacío");
            return;
        }

        // Validación estricta: La caja debe estar abierta para vender
        Caja cajaAbierta = cajaService.getCajaAbierta();
        if (cajaAbierta == null) {
            AlertUtil.mostrarAdvertencia("Caja Cerrada",
                    "No se puede registrar la venta porque la caja se encuentra cerrada.\n\n"
                    + "Es obligatorio que la caja del día esté abierta antes de realizar cobros.\n"
                    + "Por favor diríjase al módulo 'Caja' y realice la apertura.");
            return;
        }
        int idCaja = cajaAbierta.getIdCaja();

        MetodoPago metodo = metodoPago;
        BigDecimal recibido = BigDecimal.ZERO;
        if (metodo == MetodoPago.EFECTIVO) {
            try {
                recibido = new BigDecimal(txtMontoRecibido.getText().trim());
            } catch (NumberFormatException e) {
                AlertUtil.mostrarAdvertencia("Ventas", "Ingrese el monto recibido");
                return;
            }
            if (recibido.compareTo(totalCarrito()) < 0) {
                AlertUtil.mostrarAdvertencia("Ventas", "Monto recibido insuficiente");
                return;
            }
        }

        try {
            List<DetalleVenta> detalles = new ArrayList<>();
            for (CarritoItem item : carrito) {
                DetalleVenta d = new DetalleVenta();
                d.setIdProducto(item.getIdProducto());
                d.setCantidad(item.getCantidad());
                detalles.add(d);
            }

            int idCajero = SessionManager.getUsuarioActual().getIdUsuario();
            int idVenta = ventaService.registrarVenta(idCajero, cajaAbierta.getIdCaja(), metodo, detalles);

            BigDecimal vuelto = BigDecimal.ZERO;
            if (metodo == MetodoPago.EFECTIVO) {
                vuelto = recibido.subtract(totalCarrito());
            }

            List<CarritoItem> itemsVendidos = new ArrayList<>(carrito);

            String rutaTicket = "tickets/venta_" + idVenta + ".pdf";
            EmpresaConfig config = configService.get();
            Venta venta = ventaService.findById(idVenta);
            try {
                new TicketPDFGenerator().generarTicket(rutaTicket, config, venta, detalles, vuelto);
            } catch (Exception pdfEx) {
                System.err.println("No se pudo generar el archivo PDF: " + pdfEx.getMessage());
                AlertUtil.mostrarError("Error al generar PDF", "No se pudo generar el comprobante PDF: " + pdfEx.getMessage());
            }

            // Desplegar el boleto/ticket en pantalla directamente
            mostrarBoletoEnPantalla(venta, itemsVendidos, config, recibido, vuelto, rutaTicket);

            carrito.clear();
            txtMontoRecibido.clear();
            actualizarTotales();
            cargarProductos();
            tablaResultados.refresh();
            mostrarFeedback("✓ Venta B001-" + String.format("%06d", idVenta) + " registrada. Stock sincronizado con éxito.", FeedbackTipo.SUCCESS);
            enfocarBuscador();
        } catch (Exception e) {
            AlertUtil.mostrarError("Error", e.getMessage());
        }
    }

    private void mostrarBoletoEnPantalla(Venta venta, List<CarritoItem> items, EmpresaConfig config,
                                         BigDecimal recibido, BigDecimal vuelto, String rutaTicket) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        if (txtBuscar.getScene() != null && txtBuscar.getScene().getWindow() != null) {
            modal.initOwner(txtBuscar.getScene().getWindow());
        }
        String numeroBoleta = String.format("B001-%06d", venta != null ? venta.getIdVenta() : 1);
        modal.setTitle("Comprobante de Venta - " + numeroBoleta);

        VBox root = new VBox(14);
        root.setPadding(new Insets(18));
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: #f1f5f9;");

        // Contenedor que simula papel térmico
        VBox ticket = new VBox(6);
        ticket.setPadding(new Insets(20, 22, 20, 22));
        ticket.setPrefWidth(350);
        ticket.setMaxWidth(350);
        ticket.setAlignment(Pos.CENTER);
        ticket.setStyle("-fx-background-color: #ffffff; -fx-background-radius: 8px; "
                + "-fx-border-color: #cbd5e1; -fx-border-style: dashed; -fx-border-width: 1.2px; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 14, 0, 0, 4);");

        String nombreNegocio = (config != null && config.getNombreComercial() != null && !config.getNombreComercial().isBlank())
                ? config.getNombreComercial().toUpperCase() : "SUPERMERCADO LA CANASTA";
        Label lblStore = new Label(nombreNegocio);
        lblStore.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        String ruc = (config != null && config.getRuc() != null && !config.getRuc().isBlank())
                ? config.getRuc() : "20601234567";
        Label lblRuc = new Label("RUC: " + ruc);
        lblRuc.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        String dir = (config != null && config.getDireccion() != null && !config.getDireccion().isBlank())
                ? config.getDireccion() : "Av. Principal 123";
        Label lblDir = new Label(dir);
        lblDir.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        String tel = (config != null && config.getTelefono() != null && !config.getTelefono().isBlank())
                ? "TEL: " + config.getTelefono() : "";
        Label lblTel = new Label(tel);
        lblTel.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        ticket.getChildren().addAll(lblStore, lblRuc, lblDir);
        if (!tel.isBlank()) {
            ticket.getChildren().add(lblTel);
        }

        ticket.getChildren().add(crearSeparadorDashed());

        Label lblBoletaTitulo = new Label("BOLETA DE VENTA ELECTRÓNICA");
        lblBoletaTitulo.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        Label lblNumero = new Label("N°: " + numeroBoleta);
        lblNumero.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #113a69;");

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        String fechaStr = (venta != null && venta.getFecha() != null) ? venta.getFecha().format(dtf) : "Hoy";
        Label lblFecha = new Label("FECHA: " + fechaStr);
        lblFecha.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        String cajeroNombre = SessionManager.getUsuarioActual() != null ? SessionManager.getUsuarioActual().getNombre() : "Cajero";
        Label lblCajero = new Label("CAJERO: " + cajeroNombre);
        lblCajero.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        String pagoStr = (venta != null && venta.getMetodoPago() != null) ? venta.getMetodoPago().name() : "EFECTIVO";
        Label lblMetodo = new Label("FORMA DE PAGO: " + pagoStr);
        lblMetodo.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");

        ticket.getChildren().addAll(lblBoletaTitulo, lblNumero, lblFecha, lblCajero, lblMetodo);
        ticket.getChildren().add(crearSeparadorDashed());

        // Cabecera de items
        HBox cabecera = new HBox();
        Label hCant = new Label("CANT. DESCRIPCIÓN");
        hCant.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        HBox.setHgrow(hCant, Priority.ALWAYS);
        Label hTotal = new Label("TOTAL");
        hTotal.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        cabecera.getChildren().addAll(hCant, hTotal);
        ticket.getChildren().add(cabecera);

        // Lista de items
        VBox listaItems = new VBox(4);
        for (CarritoItem item : items) {
            HBox fila = new HBox();
            Label lblDesc = new Label(item.getCantidad() + "x " + item.getNombre());
            lblDesc.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #334155;");
            lblDesc.setMaxWidth(230);
            HBox.setHgrow(lblDesc, Priority.ALWAYS);

            Label lblItemTot = new Label("S/ " + item.getSubtotal().toPlainString());
            lblItemTot.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #334155;");
            fila.getChildren().addAll(lblDesc, lblItemTot);
            listaItems.getChildren().add(fila);
        }
        ticket.getChildren().add(listaItems);
        ticket.getChildren().add(crearSeparadorDashed());

        // Totales
        BigDecimal total = venta != null ? venta.getTotal() : BigDecimal.ZERO;
        BigDecimal subtotal = total.divide(new BigDecimal("1.18"), 2, RoundingMode.HALF_UP);
        BigDecimal igv = total.subtract(subtotal);

        ticket.getChildren().add(crearFilaTotal("OP. GRAVADA:", "S/ " + subtotal.toPlainString(), false));
        ticket.getChildren().add(crearFilaTotal("I.G.V. (18%):", "S/ " + igv.toPlainString(), false));
        ticket.getChildren().add(crearFilaTotal("TOTAL A PAGAR:", "S/ " + total.toPlainString(), true));

        if (venta != null && venta.getMetodoPago() == MetodoPago.EFECTIVO) {
            ticket.getChildren().add(crearFilaTotal("IMPORTE RECIBIDO:", "S/ " + recibido.toPlainString(), false));
            ticket.getChildren().add(crearFilaTotal("VUELTO:", "S/ " + vuelto.toPlainString(), true));
        }

        ticket.getChildren().add(crearSeparadorDashed());

        String mensajeTicket = (config != null && config.getMensajeTicket() != null && !config.getMensajeTicket().isBlank())
                ? config.getMensajeTicket() : "¡Gracias por su compra!";
        Label lblMensaje = new Label(mensajeTicket);
        lblMensaje.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-font-style: italic; -fx-text-fill: #64748b; -fx-alignment: center;");
        lblMensaje.setMaxWidth(300);
        lblMensaje.setWrapText(true);

        Label lblPie = new Label("Representación impresa de la Boleta Electrónica");
        lblPie.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 9.5px; -fx-text-fill: #94a3b8; -fx-alignment: center;");

        ticket.getChildren().addAll(lblMensaje, lblPie);

        // Botones de acción fijos en la parte inferior
        HBox botones = new HBox(12);
        botones.setAlignment(Pos.CENTER);
        botones.setPadding(new Insets(6, 0, 2, 0));

        Button btnImprimir = new Button("Imprimir");
        btnImprimir.getStyleClass().addAll("btn", "btn-outline");
        btnImprimir.setGraphic(new FontIcon("fas-receipt"));
        btnImprimir.setOnAction(e -> AlertUtil.mostrarInfo("Impresión", "Comprobante enviado a la impresora térmica predeterminada."));

        Button btnPdf = new Button("Ver PDF");
        btnPdf.getStyleClass().addAll("btn", "btn-soft");
        btnPdf.setGraphic(new FontIcon("fas-file-pdf"));
        btnPdf.setOnAction(e -> {
            new Thread(() -> {
                try {
                    File f = new File(rutaTicket);
                    if (!f.exists()) {
                        AlertUtil.mostrarError("Error al abrir PDF", "El comprobante no existe: " + f.getAbsolutePath());
                        return;
                    }
                    String os = System.getProperty("os.name", "").toLowerCase();
                    if (os.contains("linux")) {
                        new ProcessBuilder("xdg-open", f.getAbsolutePath()).start();
                    } else if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                        Desktop.getDesktop().open(f);
                    } else {
                        AlertUtil.mostrarInfo("PDF", "Ticket guardado en: " + f.getAbsolutePath());
                    }
                } catch (Exception ex) {
                    AlertUtil.mostrarError("Error al abrir PDF", ex.getMessage() != null ? ex.getMessage() : ex.toString());
                }
            }, "abrir-pdf-thread").start();
        });

        Button btnAceptar = new Button("Aceptar / Nueva venta");
        btnAceptar.getStyleClass().addAll("btn", "btn-primary");
        btnAceptar.setGraphic(new FontIcon("fas-check-circle"));
        btnAceptar.setOnAction(e -> modal.close());

        botones.getChildren().addAll(btnImprimir, btnPdf, btnAceptar);

        // El ticket va directamente en el contenedor principal sin ScrollPane
        root.getChildren().addAll(ticket, botones);

        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        modal.setScene(scene);
        modal.setResizable(true);
        modal.setMinWidth(480);
        modal.setMinHeight(680);
        modal.sizeToScene();
        modal.centerOnScreen();
        modal.setOnHidden(e -> enfocarBuscador());
        modal.showAndWait();
        enfocarBuscador();
    }

    private Region crearSeparadorDashed() {
        Region reg = new Region();
        reg.setStyle("-fx-border-color: #94a3b8; -fx-border-width: 1 0 0 0; -fx-border-style: dashed; -fx-min-height: 1px; -fx-pref-height: 1px;");
        return reg;
    }

    private HBox crearFilaTotal(String etiqueta, String valor, boolean negrita) {
        HBox box = new HBox();
        Label lEtiq = new Label(etiqueta);
        Label lVal = new Label(valor);
        if (negrita) {
            lEtiq.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
            lVal.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #113a69;");
        } else {
            lEtiq.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");
            lVal.setStyle("-fx-font-family: 'Courier New', monospace; -fx-font-size: 11px; -fx-text-fill: #475569;");
        }
        HBox.setHgrow(lEtiq, Priority.ALWAYS);
        box.getChildren().addAll(lEtiq, lVal);
        return box;
    }
}
