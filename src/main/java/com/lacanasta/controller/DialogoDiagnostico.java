package com.lacanasta.controller;

import com.lacanasta.util.AlertUtil;
import com.lacanasta.util.DatabaseConnection;
import com.lacanasta.util.ResourceMonitor;
import com.lacanasta.util.ResourceMonitor.EstadoRecursos;
import com.lacanasta.util.ResourceMonitor.NivelEstado;
import com.lacanasta.util.ResourceMonitor.RegistroEvento;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.SortedList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.AreaChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.util.Duration;
import org.kordamp.ikonli.javafx.FontIcon;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Ventana flotante de diagnóstico en tiempo real completamente en español.
 * Muestra tarjetas con semáforo, gráfica deslizante y una tabla de registro
 * con el consumo exacto de memoria y procesador al abrir cada módulo o evento.
 */
public final class DialogoDiagnostico {

    private static final int MAX_PUNTOS_GRAFICA = 30; // ~1 minuto (cada 2 segundos)
    private static final DateTimeFormatter HORA_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static Stage ventanaAbierta;

    private DialogoDiagnostico() {
    }

    /**
     * Abre la ventana flotante (no bloqueante) de diagnóstico en tiempo real,
     * permitiendo seguir operando el POS mientras se observan gráficas y tabla.
     *
     * @param owner ventana padre.
     */
    @SuppressWarnings("unchecked")
    public static void mostrar(Window owner) {
        if (ventanaAbierta != null && ventanaAbierta.isShowing()) {
            ventanaAbierta.toFront();
            ventanaAbierta.requestFocus();
            return;
        }

        Stage stage = new Stage();
        // Sin initOwner para que sea una ventana independiente: permite minimizarla
        // y pasar libremente a la ventana principal del POS sin quedar encima.
        stage.initModality(Modality.NONE);
        stage.setTitle("Diagnóstico y Salud del Equipo — La Canasta");
        stage.setMinWidth(880);
        stage.setMinHeight(520);
        ventanaAbierta = stage;

        // --- Cabecera ---
        Label lblTitulo = new Label("Salud y Rendimiento del Equipo");
        lblTitulo.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Label lblSubtitulo = new Label("Monitoreo en vivo mientras usas el sistema (se actualiza cada 2 segundos).");
        lblSubtitulo.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #64748b;");

        Label lblBadgeGeneral = new Label("Estado general: Óptimo");
        lblBadgeGeneral.setStyle("-fx-padding: 6 14; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12.5px;");

        VBox tituloBox = new VBox(2, lblTitulo, lblSubtitulo);
        Region spacerHeader = new Region();
        HBox.setHgrow(spacerHeader, Priority.ALWAYS);
        HBox header = new HBox(12, tituloBox, spacerHeader, lblBadgeGeneral);
        header.setAlignment(Pos.CENTER_LEFT);

        // --- 4 Tarjetas de indicadores en español ---
        TarjetaMetrica cardMemoriaDatos = new TarjetaMetrica(
                "Memoria en uso (datos)",
                "Productos, ventas y pantallas abiertas",
                "Recomendado: menos de " + ResourceMonitor.UMBRAL_MEMORIA_DATOS_ATENCION_MB + " MB"
        );
        TarjetaMetrica cardProcesador = new TarjetaMetrica(
                "Uso del procesador",
                "Esfuerzo actual del equipo",
                "Normal en reposo: menos de 15 %"
        );
        TarjetaMetrica cardMemoriaSistema = new TarjetaMetrica(
                "Memoria del sistema",
                "Estructura base del programa",
                "Recomendado: menos de " + ResourceMonitor.UMBRAL_MEMORIA_SISTEMA_ATENCION_MB + " MB"
        );
        TarjetaMetrica cardTareas = new TarjetaMetrica(
                "Tareas y componentes",
                "Procesos activos del aplicativo",
                "Normal: menos de " + ResourceMonitor.UMBRAL_TAREAS_ATENCION + " tareas"
        );

        HBox filaTarjetas = new HBox(12,
                cardMemoriaDatos.root,
                cardProcesador.root,
                cardMemoriaSistema.root,
                cardTareas.root
        );
        for (TarjetaMetrica t : new TarjetaMetrica[]{cardMemoriaDatos, cardProcesador, cardMemoriaSistema, cardTareas}) {
            HBox.setHgrow(t.root, Priority.ALWAYS);
        }

        // --- Izquierda: Gráfica animada en tiempo real ---
        CategoryAxis ejeX = new CategoryAxis();
        ejeX.setLabel("Hora");
        ejeX.setAnimated(false);

        NumberAxis ejeY = new NumberAxis();
        ejeY.setLabel("Valor (MB / %)");
        ejeY.setAutoRanging(true);

        AreaChart<String, Number> grafica = new AreaChart<>(ejeX, ejeY);
        grafica.setTitle("Historial en tiempo real (último minuto)");
        grafica.setAnimated(false);
        grafica.setCreateSymbols(false);
        grafica.setLegendVisible(true);
        grafica.setPrefHeight(220);
        VBox.setVgrow(grafica, Priority.ALWAYS);

        XYChart.Series<String, Number> serieMemoria = new XYChart.Series<>();
        serieMemoria.setName("Memoria en uso (MB)");

        XYChart.Series<String, Number> serieProcesador = new XYChart.Series<>();
        serieProcesador.setName("Procesador (%)");

        grafica.getData().addAll(serieMemoria, serieProcesador);

        VBox contenedorGrafica = new VBox(grafica);
        contenedorGrafica.setPrefWidth(390);
        contenedorGrafica.setMinWidth(320);
        contenedorGrafica.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; "
                        + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 8;"
        );
        HBox.setHgrow(contenedorGrafica, Priority.SOMETIMES);
        VBox.setVgrow(contenedorGrafica, Priority.ALWAYS);

        // --- Derecha: Tabla de actividad y consumo en ese instante ---
        Label lblTituloTabla = new Label("Registro de actividad y consumo");
        lblTituloTabla.setStyle("-fx-font-size: 13.5px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

        Button btnLimpiarTabla = new Button("Limpiar");
        btnLimpiarTabla.getStyleClass().addAll("btn", "btn-ghost");
        btnLimpiarTabla.setStyle("-fx-font-size: 11px; -fx-padding: 3 8;");

        Region spacerTabla = new Region();
        HBox.setHgrow(spacerTabla, Priority.ALWAYS);
        HBox headerTabla = new HBox(8, lblTituloTabla, spacerTabla, btnLimpiarTabla);
        headerTabla.setAlignment(Pos.CENTER_LEFT);

        ObservableList<RegistroEvento> eventosObservable = FXCollections.observableArrayList();
        SortedList<RegistroEvento> eventosOrdenados = new SortedList<>(eventosObservable);
        TableView<RegistroEvento> tablaEventos = new TableView<>(eventosOrdenados);
        eventosOrdenados.comparatorProperty().bind(tablaEventos.comparatorProperty());
        tablaEventos.setPlaceholder(new Label("Navega por los módulos del POS para ver el consumo aquí."));
        tablaEventos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaEventos.setStyle("-fx-font-size: 12px;");
        VBox.setVgrow(tablaEventos, Priority.ALWAYS);

        TableColumn<RegistroEvento, String> colHora = new TableColumn<>("Hora");
        colHora.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().hora()));
        colHora.setPrefWidth(82);
        colHora.setMinWidth(78);

        TableColumn<RegistroEvento, String> colAccion = new TableColumn<>("Acción / Módulo");
        colAccion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().accion()));
        colAccion.setPrefWidth(195);
        colAccion.setMinWidth(160);

        TableColumn<RegistroEvento, String> colTiempo = new TableColumn<>("Tiempo");
        colTiempo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().duracion()));
        colTiempo.setComparator((a, b) -> Long.compare(extraerMs(a), extraerMs(b)));
        colTiempo.setPrefWidth(76);
        colTiempo.setMinWidth(70);

        TableColumn<RegistroEvento, Number> colMemoria = new TableColumn<>("Memoria");
        colMemoria.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().memoriaMB()));
        colMemoria.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.longValue() + " MB");
            }
        });
        colMemoria.setPrefWidth(80);
        colMemoria.setMinWidth(74);

        TableColumn<RegistroEvento, Number> colCpu = new TableColumn<>("CPU");
        colCpu.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().procesadorPct()));
        colCpu.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(Number item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : String.format("%.1f%%", item.doubleValue()));
            }
        });
        colCpu.setPrefWidth(68);
        colCpu.setMinWidth(62);

        TableColumn<RegistroEvento, String> colEstado = new TableColumn<>("Estado");
        colEstado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().estado().getEtiqueta()));
        colEstado.setPrefWidth(84);
        colEstado.setMinWidth(78);
        colEstado.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || getTableRow() == null || getTableRow().getItem() == null) {
                    setText(null);
                    setStyle("");
                } else {
                    NivelEstado nivel = getTableRow().getItem().estado();
                    setText(item);
                    setStyle("-fx-font-weight: bold; -fx-text-fill: " + nivel.getColorTexto() + ";");
                }
            }
        });

        tablaEventos.getColumns().addAll(colHora, colAccion, colTiempo, colMemoria, colCpu, colEstado);

        btnLimpiarTabla.setOnAction(e -> {
            ResourceMonitor.limpiarHistorialEventos();
            eventosObservable.clear();
        });

        VBox contenedorTabla = new VBox(8, headerTabla, tablaEventos);
        contenedorTabla.setPrefWidth(600);
        contenedorTabla.setMinWidth(540);
        contenedorTabla.setStyle(
                "-fx-background-color: white; -fx-background-radius: 12; "
                        + "-fx-border-color: #e2e8f0; -fx-border-radius: 12; -fx-padding: 10;"
        );
        HBox.setHgrow(contenedorTabla, Priority.ALWAYS);

        HBox zonaCentral = new HBox(12, contenedorGrafica, contenedorTabla);
        VBox.setVgrow(zonaCentral, Priority.ALWAYS);

        // --- Barra de servicios y disco ---
        Label lblBaseDatos = new Label("Base de datos: Verificando...");
        lblBaseDatos.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #334155;");

        Label lblNube = new Label("Reporte en la nube: Verificando...");
        lblNube.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #334155;");

        Label lblDisco = new Label("Espacio libre en disco: --");
        lblDisco.setStyle("-fx-font-size: 12.5px; -fx-text-fill: #334155;");

        HBox barraServicios = new HBox(24, lblBaseDatos, lblNube, lblDisco);
        barraServicios.setAlignment(Pos.CENTER_LEFT);
        barraServicios.setStyle(
                "-fx-background-color: #f8fafc; -fx-background-radius: 10; "
                        + "-fx-border-color: #e2e8f0; -fx-border-radius: 10; -fx-padding: 10 14;"
        );

        // --- Botones de acción en español ---
        final long[] ultimaLatenciaBd = {ResourceMonitor.medirBaseDatosMs()};
        final EstadoRecursos[] ultimoEstado = {ResourceMonitor.capturarEstado()};

        Button btnLimpiarMemoria = new Button("Limpiar memoria");
        btnLimpiarMemoria.getStyleClass().addAll("btn", "btn-soft");
        btnLimpiarMemoria.setGraphic(new FontIcon("fas-broom"));

        Button btnEnviarReporte = new Button("Enviar reporte de estado");
        btnEnviarReporte.getStyleClass().addAll("btn", "btn-outline");
        btnEnviarReporte.setGraphic(new FontIcon("fas-paper-plane"));

        Region spacerFooter = new Region();
        HBox.setHgrow(spacerFooter, Priority.ALWAYS);

        Button btnCerrar = new Button("Cerrar");
        btnCerrar.getStyleClass().addAll("btn", "btn-primary");
        btnCerrar.setOnAction(e -> stage.close());

        HBox footer = new HBox(10, btnLimpiarMemoria, btnEnviarReporte, spacerFooter, btnCerrar);
        footer.setAlignment(Pos.CENTER_LEFT);

        // --- Función de actualización en vivo ---
        Runnable actualizar = () -> {
            EstadoRecursos est = ResourceMonitor.capturarEstado();
            ultimoEstado[0] = est;

            aplicarBadge(lblBadgeGeneral, "Estado general: " + est.nivelGeneral().getEtiqueta(), est.nivelGeneral());

            double progMemDatos = Math.min(1.0, (double) est.memoriaDatosMB() / ResourceMonitor.UMBRAL_MEMORIA_DATOS_CRITICO_MB);
            cardMemoriaDatos.actualizar(
                    est.memoriaDatosMB() + " MB",
                    progMemDatos,
                    est.nivelMemoriaDatos()
            );

            double progCpu = Math.min(1.0, est.procesadorPorcentaje() / 100.0);
            cardProcesador.actualizar(
                    String.format("%.1f %%", est.procesadorPorcentaje()),
                    progCpu,
                    est.nivelProcesador()
            );

            double progMemSist = Math.min(1.0, (double) est.memoriaSistemaMB() / ResourceMonitor.UMBRAL_MEMORIA_SISTEMA_CRITICO_MB);
            cardMemoriaSistema.actualizar(
                    est.memoriaSistemaMB() + " MB",
                    progMemSist,
                    est.nivelMemoriaSistema()
            );

            double progTareas = Math.min(1.0, (double) est.tareasSimultaneas() / ResourceMonitor.UMBRAL_TAREAS_CRITICO);
            cardTareas.actualizar(
                    est.tareasSimultaneas() + " tareas • " + est.componentesCargados() + " comp.",
                    progTareas,
                    est.nivelTareas().ordinal() >= est.nivelComponentes().ordinal()
                            ? est.nivelTareas() : est.nivelComponentes()
            );

            // Actualizar gráfica deslizante
            String hora = LocalTime.now().format(HORA_FMT);
            serieMemoria.getData().add(new XYChart.Data<>(hora, est.memoriaDatosMB()));
            serieProcesador.getData().add(new XYChart.Data<>(hora, Math.round(est.procesadorPorcentaje() * 10.0) / 10.0));
            if (serieMemoria.getData().size() > MAX_PUNTOS_GRAFICA) {
                serieMemoria.getData().remove(0);
                serieProcesador.getData().remove(0);
            }

            // Actualizar tabla SOLO si realmente llegaron eventos nuevos,
            // conservando el ordenamiento actual del usuario (SortedList) sin perder la vista.
            var historialActual = ResourceMonitor.getHistorialEventos();
            if (!historialActual.equals(eventosObservable)) {
                RegistroEvento seleccionado = tablaEventos.getSelectionModel().getSelectedItem();
                eventosObservable.setAll(historialActual);
                if (seleccionado != null) {
                    tablaEventos.getSelectionModel().select(seleccionado);
                }
            }

            // Servicios
            long bdMs = ultimaLatenciaBd[0];
            String nombreBd = DatabaseConnection.getBaseDatosActiva();
            String etiquetaModo = DatabaseConnection.isModoPruebaActivo() ? " [MODO PRUEBA]" : "";
            if (bdMs >= 0) {
                lblBaseDatos.setText("Base de datos (" + nombreBd + ")" + etiquetaModo + ": Conectada (" + bdMs + " ms)");
                lblBaseDatos.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: "
                        + (DatabaseConnection.isModoPruebaActivo() ? "#d97706;" : "#059669;"));
            } else {
                lblBaseDatos.setText("Base de datos (" + nombreBd + "): Sin conexión");
                lblBaseDatos.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #dc2626;");
            }

            if (est.reporteNubeActivo()) {
                lblNube.setText("Reporte en la nube: Activo (" + est.reportesPendientesNube() + " en espera)");
            } else {
                lblNube.setText("Reporte en la nube: Desactivado");
            }
            lblDisco.setText("Espacio libre en disco: " + est.discoLibreGB() + " GB");
        };

        btnLimpiarMemoria.setOnAction(e -> {
            long t0 = System.currentTimeMillis();
            long antes = ultimoEstado[0].memoriaDatosMB();
            System.gc();
            ultimaLatenciaBd[0] = ResourceMonitor.medirBaseDatosMs();
            ResourceMonitor.registrarActividad("Limpieza de memoria", System.currentTimeMillis() - t0);
            actualizar.run();
            long despues = ultimoEstado[0].memoriaDatosMB();
            long liberado = Math.max(0, antes - despues);
            AlertUtil.mostrarInfo(
                    "Limpieza de memoria",
                    "Limpieza completada.\nMemoria antes: " + antes + " MB\nMemoria actual: " + despues + " MB"
                            + (liberado > 0 ? "\nSe liberaron " + liberado + " MB." : "")
            );
        });

        btnEnviarReporte.setOnAction(e -> {
            ultimaLatenciaBd[0] = ResourceMonitor.medirBaseDatosMs();
            actualizar.run();
            if (!ultimoEstado[0].reporteNubeActivo()) {
                AlertUtil.mostrarAdvertencia(
                        "Reporte de estado",
                        "El reporte en la nube está desactivado en este equipo (falta configurar sentry.properties)."
                );
                return;
            }
            ResourceMonitor.enviarReporteManual(ultimoEstado[0], ultimaLatenciaBd[0]);
            ResourceMonitor.registrarActividad("Reporte enviado a la nube", -1);
            actualizar.run();
            AlertUtil.mostrarInfo(
                    "Reporte enviado",
                    "Se envió el estado actual del equipo al panel de monitoreo.\n"
                            + "(Si no hay internet, quedará guardado y se enviará al reconectar)."
            );
        });

        // Primera lectura inmediata + actualización cada 2 segundos
        actualizar.run();
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(2), e -> actualizar.run()));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        stage.setOnHidden(e -> {
            timeline.stop();
            ventanaAbierta = null;
        });

        VBox root = new VBox(12, header, filaTarjetas, zonaCentral, barraServicios, footer);
        root.setPadding(new Insets(14));
        root.setStyle("-fx-background-color: #f1f5f9;");

        Scene scene = new Scene(root, 1020, 580);
        var css = DialogoDiagnostico.class.getResource("/css/styles.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        }
        stage.setScene(scene);
        stage.centerOnScreen();
        stage.show();
    }

    private static long extraerMs(String texto) {
        if (texto == null) {
            return -1;
        }
        String digitos = texto.replaceAll("[^0-9]", "");
        if (digitos.isEmpty()) {
            return -1;
        }
        try {
            return Long.parseLong(digitos);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static void aplicarBadge(Label label, String texto, NivelEstado nivel) {
        label.setText(texto);
        label.setStyle(
                "-fx-padding: 5 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12px; "
                        + "-fx-text-fill: " + nivel.getColorTexto() + "; "
                        + "-fx-background-color: " + nivel.getColorFondo() + ";"
        );
    }

    /**
     * Tarjeta visual compacta para cada uno de los 4 indicadores.
     */
    private static final class TarjetaMetrica {
        final VBox root;
        private final Label lblValor;
        private final Label lblEstado;
        private final ProgressBar barra;

        TarjetaMetrica(String titulo, String descripcion, String referencia) {
            Label lblTitulo = new Label(titulo);
            lblTitulo.setStyle("-fx-font-size: 12.5px; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            lblEstado = new Label("Óptimo");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            HBox top = new HBox(6, lblTitulo, spacer, lblEstado);
            top.setAlignment(Pos.CENTER_LEFT);

            lblValor = new Label("--");
            lblValor.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #0f172a;");

            barra = new ProgressBar(0);
            barra.setMaxWidth(Double.MAX_VALUE);
            barra.setPrefHeight(8);

            Label lblDesc = new Label(descripcion);
            lblDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b;");

            Label lblRef = new Label(referencia);
            lblRef.setStyle("-fx-font-size: 10.5px; -fx-text-fill: #94a3b8;");

            root = new VBox(6, top, lblValor, barra, lblDesc, lblRef);
            root.setPadding(new Insets(12));
            root.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 12; "
                            + "-fx-border-color: #e2e8f0; -fx-border-radius: 12;"
            );
        }

        void actualizar(String valorTexto, double progreso, NivelEstado nivel) {
            lblValor.setText(valorTexto);
            barra.setProgress(Math.max(0.02, Math.min(1.0, progreso)));
            barra.setStyle("-fx-accent: " + nivel.getColorTexto() + ";");
            aplicarBadge(lblEstado, nivel.getEtiqueta(), nivel);
        }
    }
}
