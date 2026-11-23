package com.lacanasta.util;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utilidad para cargar, cachear y renderizar imágenes de productos con esquinas redondeadas
 * y placeholders elegantes en caso de ausencia o error de carga.
 */
public final class ProductImageHelper {

    private static final Map<String, Image> CACHE = new ConcurrentHashMap<>();

    private static final List<String> PRESET_IMAGES = Arrays.asList(
            "/images/products/coca_cola.png",
            "/images/products/inca_kola.png",
            "/images/products/agua_san_luis.png",
            "/images/products/leche_gloria.png",
            "/images/products/yogurt_gloria.png",
            "/images/products/queso_laive.png",
            "/images/products/arroz_costeno.png",
            "/images/products/fideos_don_vittorio.png",
            "/images/products/aceite_primor.png",
            "/images/products/atun_fanny.png",
            "/images/products/cafe_nescafe.png",
            "/images/products/pan_bimbo.png",
            "/images/products/galletas_oreo.png",
            "/images/products/chocolate_sublime.png",
            "/images/products/papas_lays.png",
            "/images/products/detergente_bolivar.png",
            "/images/products/lejia_clorox.png",
            "/images/products/jabon_aval.png"
    );

    private ProductImageHelper() {
    }

    /**
     * Obtiene la lista de imágenes predeterminadas disponibles en el catálogo.
     */
    public static List<String> getPresetImages() {
        return new ArrayList<>(PRESET_IMAGES);
    }

    /**
     * Carga una imagen con soporte para caché en memoria.
     * Soporta rutas relativas del classpath (ej. /images/products/coca_cola.png),
     * rutas de archivo locales y URLs.
     */
    public static Image getImage(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }

        String key = url.trim();
        return CACHE.computeIfAbsent(key, k -> {
            try {
                // 1. Probar recurso en el classpath
                String resPath = k.startsWith("/") ? k : "/" + k;
                InputStream is = ProductImageHelper.class.getResourceAsStream(resPath);
                if (is != null) {
                    return new Image(is);
                }

                // 2. Probar archivo en el sistema de archivos
                File f = new File(k);
                if (f.exists() && f.isFile()) {
                    return new Image(f.toURI().toString());
                }

                // 3. Probar URL directa
                if (k.startsWith("http://") || k.startsWith("https://") || k.startsWith("file:")) {
                    return new Image(k, true);
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    /**
     * Genera un nodo de miniatura con esquinas redondeadas y borde sutil.
     *
     * @param url    ruta de la imagen.
     * @param width  ancho deseado.
     * @param height alto deseado.
     * @param arc    radio de curvatura de las esquinas.
     * @return un contenedor {@link Node} estilizado.
     */
    public static Node createThumbnail(String url, double width, double height, double arc) {
        StackPane container = new StackPane();
        container.setPrefSize(width, height);
        container.setMinSize(width, height);
        container.setMaxSize(width, height);
        container.setAlignment(Pos.CENTER);

        container.setStyle("-fx-background-color: #f1f5f9; -fx-background-radius: " + arc
                + "px; -fx-border-color: #e2e8f0; -fx-border-radius: " + arc
                + "px; -fx-border-width: 1px;");

        Image img = getImage(url);
        if (img != null && !img.isError()) {
            ImageView iv = new ImageView(img);
            iv.setFitWidth(width - 4);
            iv.setFitHeight(height - 4);
            iv.setPreserveRatio(true);
            iv.setSmooth(true);

            Rectangle clip = new Rectangle(width - 4, height - 4);
            clip.setArcWidth(arc);
            clip.setArcHeight(arc);
            iv.setClip(clip);

            container.getChildren().add(iv);
        } else {
            // Placeholder con icono
            FontIcon icon;
            try {
                icon = new FontIcon("fas-boxes");
            } catch (Exception e) {
                icon = new FontIcon("bi-box-seam");
            }
            icon.setIconSize((int) Math.max(14, width * 0.45));
            icon.setIconColor(Color.web("#94a3b8"));
            container.getChildren().add(icon);
        }

        return container;
    }

    /**
     * Versión simplificada con radio de 8px por defecto.
     */
    public static Node createThumbnail(String url, double size) {
        return createThumbnail(url, size, size, 8.0);
    }
}
