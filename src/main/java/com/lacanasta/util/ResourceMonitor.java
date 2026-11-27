package com.lacanasta.util;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

import java.io.File;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Monitor automático de recursos del equipo (memoria, procesador, tareas y componentes).
 * Evalúa umbrales en dos niveles (Atención y Crítico) y reporta incidencias a Sentry
 * sin afectar el funcionamiento normal del punto de venta.
 */
public final class ResourceMonitor {

    // Umbrales recomendados basados en la línea base real del POS
    public static final long UMBRAL_MEMORIA_DATOS_ATENCION_MB = 250;
    public static final long UMBRAL_MEMORIA_DATOS_CRITICO_MB = 400;

    public static final long UMBRAL_MEMORIA_SISTEMA_ATENCION_MB = 120;
    public static final long UMBRAL_MEMORIA_SISTEMA_CRITICO_MB = 180;

    public static final double UMBRAL_PROCESADOR_ATENCION_PCT = 75.0;
    public static final double UMBRAL_PROCESADOR_CRITICO_PCT = 90.0;

    public static final int UMBRAL_TAREAS_ATENCION = 60;
    public static final int UMBRAL_TAREAS_CRITICO = 100;

    public static final int UMBRAL_COMPONENTES_ATENCION = 15_000;
    public static final int UMBRAL_COMPONENTES_CRITICO = 20_000;

    private static final long INTERVALO_SEGUNDOS = 15;
    private static final long COOLDOWN_MS = TimeUnit.MINUTES.toMillis(5);
    private static final int MAX_HISTORIAL_EVENTOS = 100;
    private static final DateTimeFormatter HORA_EVENTO_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    public static final long UMBRAL_MODULO_ATENCION_MS = 500;
    public static final long UMBRAL_MODULO_CRITICO_MS = 1_500;

    private static ScheduledExecutorService scheduler;
    private static final Map<String, Long> ultimaAlertaPorTipo = new ConcurrentHashMap<>();
    private static final Deque<RegistroEvento> historialEventos = new ConcurrentLinkedDeque<>();
    private static int lecturasCpuAltaConsecutivas = 0;

    private ResourceMonitor() {
    }

    /**
     * Semáforo de salud comprensible para el administrador.
     */
    public enum NivelEstado {
        OPTIMO("Óptimo", "#10b981", "#ecfdf5"),
        ATENCION("Atención", "#d97706", "#fffbeb"),
        CRITICO("Crítico", "#dc2626", "#fef2f2");

        private final String etiqueta;
        private final String colorTexto;
        private final String colorFondo;

        NivelEstado(String etiqueta, String colorTexto, String colorFondo) {
            this.etiqueta = etiqueta;
            this.colorTexto = colorTexto;
            this.colorFondo = colorFondo;
        }

        public String getEtiqueta() {
            return etiqueta;
        }

        public String getColorTexto() {
            return colorTexto;
        }

        public String getColorFondo() {
            return colorFondo;
        }
    }

    /**
     * Fotografía instantánea del estado de recursos del equipo.
     */
    public record EstadoRecursos(
            long memoriaDatosMB,
            long memoriaDatosMaxMB,
            long memoriaSistemaMB,
            double procesadorPorcentaje,
            int tareasSimultaneas,
            int picoTareas,
            int componentesCargados,
            long discoLibreGB,
            boolean reporteNubeActivo,
            int reportesPendientesNube,
            NivelEstado nivelMemoriaDatos,
            NivelEstado nivelMemoriaSistema,
            NivelEstado nivelProcesador,
            NivelEstado nivelTareas,
            NivelEstado nivelComponentes,
            NivelEstado nivelGeneral
    ) {
    }

    /**
     * Registro puntual de una acción o alerta junto con el consumo en ese instante.
     */
    public record RegistroEvento(
            String hora,
            String accion,
            String duracion,
            long memoriaMB,
            double procesadorPct,
            NivelEstado estado
    ) {
    }

    /**
     * Inicia el monitoreo periódico en un hilo en segundo plano (daemon).
     */
    public static synchronized void iniciar() {
        if (scheduler != null && !scheduler.isShutdown()) {
            return;
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "monitor-recursos-pos");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(ResourceMonitor::revisarYAlertar, 20, INTERVALO_SEGUNDOS, TimeUnit.SECONDS);
    }

    /**
     * Detiene el monitoreo en segundo plano al cerrar la aplicación.
     */
    public static synchronized void detener() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        lecturasCpuAltaConsecutivas = 0;
    }

    /**
     * Obtiene una lectura actual de los 4 indicadores principales y del entorno.
     */
    public static EstadoRecursos capturarEstado() {
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryBean.getHeapMemoryUsage();
        long memoriaDatosMB = heap.getUsed() / (1024 * 1024);
        long memoriaDatosMaxMB = heap.getMax() > 0
                ? heap.getMax() / (1024 * 1024)
                : Runtime.getRuntime().maxMemory() / (1024 * 1024);

        long memoriaSistemaMB = obtenerMemoriaSistemaMB();
        double procesadorPct = obtenerProcesadorPorcentaje();

        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int tareas = threadBean.getThreadCount();
        int picoTareas = threadBean.getPeakThreadCount();

        ClassLoadingMXBean classBean = ManagementFactory.getClassLoadingMXBean();
        int componentes = classBean.getLoadedClassCount();

        long discoLibreGB = obtenerDiscoLibreGB();
        boolean nubeActiva = Sentry.isEnabled();
        int pendientesNube = contarReportesPendientesNube();

        NivelEstado nMemDatos = evaluarNivel(memoriaDatosMB, UMBRAL_MEMORIA_DATOS_ATENCION_MB, UMBRAL_MEMORIA_DATOS_CRITICO_MB);
        NivelEstado nMemSist = evaluarNivel(memoriaSistemaMB, UMBRAL_MEMORIA_SISTEMA_ATENCION_MB, UMBRAL_MEMORIA_SISTEMA_CRITICO_MB);
        NivelEstado nCpu = evaluarNivel(procesadorPct, UMBRAL_PROCESADOR_ATENCION_PCT, UMBRAL_PROCESADOR_CRITICO_PCT);
        NivelEstado nTareas = evaluarNivel(tareas, UMBRAL_TAREAS_ATENCION, UMBRAL_TAREAS_CRITICO);
        NivelEstado nComp = evaluarNivel(componentes, UMBRAL_COMPONENTES_ATENCION, UMBRAL_COMPONENTES_CRITICO);

        NivelEstado general = peorNivel(nMemDatos, nMemSist, nCpu, nTareas, nComp);

        return new EstadoRecursos(
                memoriaDatosMB,
                memoriaDatosMaxMB,
                memoriaSistemaMB,
                procesadorPct,
                tareas,
                picoTareas,
                componentes,
                discoLibreGB,
                nubeActiva,
                pendientesNube,
                nMemDatos,
                nMemSist,
                nCpu,
                nTareas,
                nComp,
                general
        );
    }

    /**
     * Evalúa un valor numérico contra sus umbrales de atención y crítico.
     */
    public static NivelEstado evaluarNivel(double valor, double umbralAtencion, double umbralCritico) {
        if (valor >= umbralCritico) {
            return NivelEstado.CRITICO;
        }
        if (valor >= umbralAtencion) {
            return NivelEstado.ATENCION;
        }
        return NivelEstado.OPTIMO;
    }

    /**
     * Mide el tiempo de respuesta de la base de datos MySQL en milisegundos.
     * Devuelve -1 si no se pudo conectar.
     */
    public static long medirBaseDatosMs() {
        long inicio = System.currentTimeMillis();
        try (Connection conn = new DatabaseConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
            ps.executeQuery();
            return System.currentTimeMillis() - inicio;
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * Envía un reporte manual del estado actual del equipo a Sentry.
     */
    public static void enviarReporteManual(EstadoRecursos estado, long bdMs) {
        String resumen = construirResumenTexto("Reporte manual de diagnóstico del equipo", estado, bdMs);
        Sentry.captureMessage(resumen, SentryLevel.INFO);
    }

    /**
     * Registra una acción del sistema (ej. apertura de módulo, limpieza o cambio de modo)
     * junto con la memoria y procesador utilizados en ese preciso instante.
     *
     * @param accion     descripción en español de la acción.
     * @param duracionMs duración en milisegundos, o negativo si no aplica.
     */
    public static void registrarActividad(String accion, long duracionMs) {
        EstadoRecursos estado = capturarEstado();
        NivelEstado nivel = estado.nivelGeneral();
        if (duracionMs >= 0) {
            NivelEstado nivelTiempo = evaluarNivel(duracionMs, UMBRAL_MODULO_ATENCION_MS, UMBRAL_MODULO_CRITICO_MS);
            nivel = peorNivel(nivel, nivelTiempo);
        }
        String duracionTexto = duracionMs >= 0 ? duracionMs + " ms" : "—";
        agregarEvento(new RegistroEvento(
                LocalTime.now().format(HORA_EVENTO_FMT),
                accion,
                duracionTexto,
                estado.memoriaDatosMB(),
                estado.procesadorPorcentaje(),
                nivel
        ));
    }

    /**
     * Devuelve una copia de los eventos más recientes (más nuevo primero).
     */
    public static List<RegistroEvento> getHistorialEventos() {
        return new ArrayList<>(historialEventos);
    }

    /**
     * Limpia el historial en memoria mostrado en la tabla de Diagnóstico.
     */
    public static void limpiarHistorialEventos() {
        historialEventos.clear();
    }

    private static void agregarEvento(RegistroEvento evento) {
        historialEventos.addFirst(evento);
        while (historialEventos.size() > MAX_HISTORIAL_EVENTOS) {
            historialEventos.pollLast();
        }
    }

    private static void revisarYAlertar() {
        try {
            EstadoRecursos estado = capturarEstado();

            if (estado.nivelMemoriaDatos() != NivelEstado.OPTIMO) {
                emitirAlerta("memoria_datos", estado.nivelMemoriaDatos(),
                        "Consumo alto de memoria en uso (" + estado.memoriaDatosMB() + " MB)", estado);
            }
            if (estado.nivelMemoriaSistema() != NivelEstado.OPTIMO) {
                emitirAlerta("memoria_sistema", estado.nivelMemoriaSistema(),
                        "Consumo alto de memoria del sistema (" + estado.memoriaSistemaMB() + " MB)", estado);
            }
            if (estado.nivelProcesador() != NivelEstado.OPTIMO) {
                lecturasCpuAltaConsecutivas++;
                if (lecturasCpuAltaConsecutivas >= 2) {
                    emitirAlerta("procesador", estado.nivelProcesador(),
                            String.format("Uso sostenido alto del procesador (%.1f %%)", estado.procesadorPorcentaje()), estado);
                }
            } else {
                lecturasCpuAltaConsecutivas = 0;
            }
            if (estado.nivelTareas() != NivelEstado.OPTIMO) {
                emitirAlerta("tareas", estado.nivelTareas(),
                        "Cantidad alta de tareas simultáneas (" + estado.tareasSimultaneas() + ")", estado);
            }
            if (estado.nivelComponentes() != NivelEstado.OPTIMO) {
                emitirAlerta("componentes", estado.nivelComponentes(),
                        "Cantidad alta de componentes cargados (" + estado.componentesCargados() + ")", estado);
            }
        } catch (Exception ignored) {
            // El monitor nunca debe interrumpir la operación del POS
        }
    }

    private static void emitirAlerta(String clave, NivelEstado nivel, String titulo, EstadoRecursos estado) {
        long ahora = System.currentTimeMillis();
        Long anterior = ultimaAlertaPorTipo.get(clave);
        if (anterior != null && (ahora - anterior) < COOLDOWN_MS) {
            return;
        }
        ultimaAlertaPorTipo.put(clave, ahora);

        String detalle = construirResumenTexto(titulo, estado, -2);
        SentryLevel sentryLevel = nivel == NivelEstado.CRITICO ? SentryLevel.ERROR : SentryLevel.WARNING;
        Sentry.captureMessage(detalle, sentryLevel);
        agregarEvento(new RegistroEvento(
                LocalTime.now().format(HORA_EVENTO_FMT),
                titulo,
                "Alerta",
                estado.memoriaDatosMB(),
                estado.procesadorPorcentaje(),
                nivel
        ));

        if (nivel == NivelEstado.CRITICO) {
            AlertUtil.mostrarAdvertencia(
                    "Aviso de rendimiento del equipo",
                    titulo + ".\n\nEl sistema detectó un consumo elevado de recursos. "
                            + "Se recomienda finalizar la atención actual y usar 'Limpiar memoria' en Configuración > Diagnóstico "
                            + "o reiniciar el programa al cerrar turno."
            );
        }
    }

    private static String construirResumenTexto(String encabezado, EstadoRecursos e, long bdMs) {
        String bdTexto = bdMs == -2 ? "No medida" : (bdMs < 0 ? "Sin conexión" : bdMs + " ms");
        return String.format(
                "%s | Estado: %s | Memoria datos: %d MB | Memoria sistema: %d MB | Procesador: %.1f%% | Tareas: %d (pico %d) | Componentes: %d | Disco libre: %d GB | BD: %s",
                encabezado,
                e.nivelGeneral().getEtiqueta(),
                e.memoriaDatosMB(),
                e.memoriaSistemaMB(),
                e.procesadorPorcentaje(),
                e.tareasSimultaneas(),
                e.picoTareas(),
                e.componentesCargados(),
                e.discoLibreGB(),
                bdTexto
        );
    }

    private static long obtenerMemoriaSistemaMB() {
        for (MemoryPoolMXBean pool : ManagementFactory.getMemoryPoolMXBeans()) {
            if ("Metaspace".equalsIgnoreCase(pool.getName())) {
                return pool.getUsage().getUsed() / (1024 * 1024);
            }
        }
        return ManagementFactory.getMemoryMXBean().getNonHeapMemoryUsage().getUsed() / (1024 * 1024);
    }

    private static double obtenerProcesadorPorcentaje() {
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        if (osBean instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            double load = sunOs.getProcessCpuLoad();
            if (load >= 0.0) {
                return Math.min(100.0, load * 100.0);
            }
        }
        return 0.0;
    }

    private static long obtenerDiscoLibreGB() {
        try {
            File raiz = new File(System.getProperty("user.dir")).toPath().getRoot().toFile();
            return raiz.getUsableSpace() / (1024L * 1024L * 1024L);
        } catch (Exception e) {
            return 0;
        }
    }

    private static int contarReportesPendientesNube() {
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".lacanasta", "sentry-cache");
        if (!Files.exists(cacheDir)) {
            return 0;
        }
        try (Stream<Path> walk = Files.walk(cacheDir)) {
            return (int) walk.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".envelope"))
                    .count();
        } catch (Exception e) {
            return 0;
        }
    }

    private static NivelEstado peorNivel(NivelEstado... niveles) {
        NivelEstado peor = NivelEstado.OPTIMO;
        for (NivelEstado n : niveles) {
            if (n.ordinal() > peor.ordinal()) {
                peor = n;
            }
        }
        return peor;
    }
}
