package com.lacanasta.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lacanasta.dto.ProductoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;

/**
 * Servicio de identificación global de productos mediante escaneo de código de barras sin límites.
 * Consulta la API global de Open Food Facts y, como respaldo, UPCitemdb.
 * Descarga y almacena localmente la imagen del producto.
 */
public class ProductoGlobalService {

    private static final Logger log = LoggerFactory.getLogger(ProductoGlobalService.class);

    private static final String OPEN_FOOD_FACTS_URL = "https://world.openfoodfacts.org/api/v2/product/%s.json";
    private static final String UPCITEMDB_URL = "https://api.upcitemdb.com/prod/trial/lookup?upc=%s";

    public static final String DIRECTORY_IMG_LOCAL = "/home/jacob/PC3_JACOB_BENITO_LOPEZ/LaCanasta/productos_img";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProductoGlobalService() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), new ObjectMapper());
    }

    public ProductoGlobalService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Valida que el código sea estrictamente numérico y tenga una longitud estándar entre 8 y 14 dígitos
     * (EAN-8, UPC-E, UPC-A, EAN-13, ITF-14/GTIN-14).
     *
     * @param codigo código de barras a evaluar.
     * @return true si es numérico y tiene entre 8 y 14 dígitos.
     */
    public boolean esCodigoValido(String codigo) {
        if (codigo == null) {
            return false;
        }
        String limpio = codigo.trim();
        return limpio.matches("^\\d{8,14}$");
    }

    /**
     * Limpia el nombre del producto eliminando sufijos ruidosos (ej. " X", " x",
     * " (imported)", " (importado)"), espacios redundantes y caracteres de puntuación residuales.
     * Trunca a un máximo seguro de 100 caracteres.
     *
     * @param nombre nombre comercial en bruto.
     * @return nombre limpio y validado para MySQL.
     */
    public static String limpiarNombreProducto(String nombre) {
        if (nombre == null) {
            return "";
        }
        String limpio = nombre.trim();
        // 1. Reemplazar múltiples espacios o tabulaciones por uno solo
        limpio = limpio.replaceAll("\\s+", " ");
        // 2. Eliminar sufijos de importación
        limpio = limpio.replaceAll("(?i)\\s*\\(importado\\)$", "").trim();
        limpio = limpio.replaceAll("(?i)\\s*\\(imported\\)$", "").trim();
        limpio = limpio.replaceAll("(?i)\\s*\\(import\\)$", "").trim();
        // 3. Eliminar artefactos finales " X" o " x"
        limpio = limpio.replaceAll("(?i)\\s+[xX]$", "").trim();
        // 4. Eliminar puntuaciones o guiones sueltos al final
        limpio = limpio.replaceAll("[\\-_,/.\\s]+$", "").trim();
        // 5. Segunda pasada para " X" tras remover signos
        limpio = limpio.replaceAll("(?i)\\s+[xX]$", "").trim();

        // 6. Truncar a máximo 100 caracteres (restricción VARCHAR(100) en base de datos)
        if (limpio.length() > 100) {
            limpio = limpio.substring(0, 100).trim();
        }
        return limpio;
    }

    /**
     * Sobrecarga de conveniencia que utiliza el código como texto de respaldo si el nombre queda vacío.
     */
    public static String limpiarNombre(String nombre, String codigo) {
        String limpio = limpiarNombreProducto(nombre);
        if (limpio.isEmpty()) {
            return (codigo != null && !codigo.isBlank()) ? "Producto " + codigo.trim() : "Producto sin nombre";
        }
        return limpio;
    }

    /**
     * Descarga la imagen remota del producto y la almacena en el disco local:
     * {@code /home/jacob/PC3_JACOB_BENITO_LOPEZ/LaCanasta/productos_img/{codigo_barra}.jpg}.
     * Si la descarga falla por cualquier motivo, no bloquea el flujo y retorna null.
     *
     * @param urlImagen   URL remota de la imagen.
     * @param codigoBarra código del producto para nombrar el archivo local.
     * @return ruta absoluta local del archivo guardado, o null si falló o no había imagen.
     */
    public String descargarImagenLocal(String urlImagen, String codigoBarra) {
        if (urlImagen == null || urlImagen.isBlank() || codigoBarra == null || codigoBarra.isBlank()) {
            return null;
        }

        try {
            File dir = new File(DIRECTORY_IMG_LOCAL);
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String nombreArchivo = codigoBarra.trim() + ".jpg";
            File archivoDestino = new File(dir, nombreArchivo);

            log.info("[GLOBAL-SCAN] Descargando imagen para {}: {} -> {}", codigoBarra, urlImagen, archivoDestino.getAbsolutePath());

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlImagen))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "LaCanastaPOS/1.0 (contacto@lacanasta.pe)")
                    .GET()
                    .build();

            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200 && response.body() != null && response.body().length > 0) {
                Files.write(archivoDestino.toPath(), response.body());
                log.info("[GLOBAL-SCAN] Imagen guardada localmente con éxito: {} ({} bytes)",
                        archivoDestino.getAbsolutePath(), response.body().length);
                return archivoDestino.getAbsolutePath();
            } else {
                log.warn("[GLOBAL-SCAN] Respuesta no exitosa al descargar imagen (HTTP {}): {}", response.statusCode(), urlImagen);
            }
        } catch (Exception e) {
            log.warn("[GLOBAL-SCAN] Error no bloqueante al descargar foto del producto {}: {}", codigoBarra, e.getMessage());
        }
        return null;
    }

    /**
     * Consulta el catálogo global sin límites de país ni prefijos.
     * 1. Consulta la API global de Open Food Facts.
     * 2. Si no lo encuentra, consulta la API de respaldo UPCitemdb.
     * 3. Descarga la imagen localmente si está disponible.
     *
     * @param codigoBarra código escaneado o digitado.
     * @return ProductoDTO con los datos encontrados o con encontrado=false.
     */
    public ProductoDTO buscarGlobal(String codigoBarra) {
        ProductoDTO dto = new ProductoDTO(codigoBarra);
        if (codigoBarra == null || codigoBarra.trim().isEmpty()) {
            dto.setEncontrado(false);
            return dto;
        }

        String codigo = codigoBarra.trim();
        log.info("[GLOBAL-SCAN] Escaneando código de barras: {}", codigo);

        if (!esCodigoValido(codigo)) {
            log.warn("[GLOBAL-SCAN] Código descartado (debe contener entre 8 y 14 dígitos numéricos): '{}'", codigo);
            dto.setEncontrado(false);
            return dto;
        }

        // 1. Intentar con Open Food Facts
        ProductoDTO offDto = consultarOpenFoodFacts(codigo);
        if (offDto != null && offDto.isEncontrado()) {
            log.info("[GLOBAL-SCAN] Producto encontrado en Open Food Facts: [{}] {}", codigo, offDto.getNombre());
            return offDto;
        }

        // 2. Si no encuentra en Open Food Facts, intentar con UPCitemdb de respaldo
        log.info("[GLOBAL-SCAN] No encontrado en Open Food Facts. Consultando respaldo en UPCitemdb para: {}", codigo);
        ProductoDTO upcDto = consultarUPCitemdb(codigo);
        if (upcDto != null && upcDto.isEncontrado()) {
            log.info("[GLOBAL-SCAN] Producto encontrado en UPCitemdb: [{}] {}", codigo, upcDto.getNombre());
            return upcDto;
        }

        log.warn("[GLOBAL-SCAN] Código {} no encontrado en ninguna base global (OpenFoodFacts / UPCitemdb)", codigo);
        dto.setEncontrado(false);
        return dto;
    }

    private ProductoDTO consultarOpenFoodFacts(String codigo) {
        String url = String.format(OPEN_FOOD_FACTS_URL, codigo);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "LaCanastaPOS/1.0 (contacto@lacanasta.pe)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[GLOBAL-SCAN] [OpenFoodFacts] Código {}: HTTP {}", codigo, response.statusCode());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            int status = root.path("status").asInt(0);
            if (status != 1) {
                return null;
            }

            JsonNode prodNode = root.path("product");
            ProductoDTO dto = new ProductoDTO(codigo);
            dto.setEncontrado(true);

            // Nombre
            String nombreCrudo = prodNode.path("product_name").asText("").trim();
            if (nombreCrudo.isEmpty()) {
                nombreCrudo = prodNode.path("product_name_es").asText("").trim();
            }
            if (nombreCrudo.isEmpty()) {
                nombreCrudo = prodNode.path("product_name_en").asText("").trim();
            }
            if (nombreCrudo.isEmpty()) {
                nombreCrudo = prodNode.path("generic_name").asText("").trim();
            }
            dto.setNombre(limpiarNombre(nombreCrudo, codigo));

            // Marca
            String marca = prodNode.path("brands").asText("").trim();
            dto.setMarca(marca);

            // Categoría
            String categoria = prodNode.path("categories").asText("").trim();
            dto.setCategoria(categoria);

            // Países
            String countries = prodNode.path("countries").asText("").trim();
            dto.setPaises(countries);

            // Imagen (image_front_url o image_url)
            String imagenUrl = prodNode.path("image_front_url").asText("").trim();
            if (imagenUrl.isEmpty()) {
                imagenUrl = prodNode.path("image_url").asText("").trim();
            }
            if (imagenUrl.isEmpty()) {
                imagenUrl = prodNode.path("image_front_small_url").asText("").trim();
            }
            dto.setImagenUrl(imagenUrl);

            // Descargar imagen localmente si existe
            if (!imagenUrl.isEmpty()) {
                String rutaLocal = descargarImagenLocal(imagenUrl, codigo);
                dto.setImagenRuta(rutaLocal);
            }

            return dto;

        } catch (Exception e) {
            log.warn("[GLOBAL-SCAN] Excepción al consultar Open Food Facts para {}: {}", codigo, e.getMessage());
            return null;
        }
    }

    private ProductoDTO consultarUPCitemdb(String codigo) {
        String url = String.format(UPCITEMDB_URL, codigo);
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "LaCanastaPOS/1.0 (contacto@lacanasta.pe)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("[GLOBAL-SCAN] [UPCitemdb] Código {}: HTTP {}", codigo, response.statusCode());

            if (response.statusCode() != 200) {
                return null;
            }

            JsonNode root = objectMapper.readTree(response.body());
            String code = root.path("code").asText("");
            int total = root.path("total").asInt(0);

            if (!"OK".equalsIgnoreCase(code) || total <= 0) {
                return null;
            }

            JsonNode items = root.path("items");
            if (!items.isArray() || items.isEmpty()) {
                return null;
            }

            JsonNode item = items.get(0);
            ProductoDTO dto = new ProductoDTO(codigo);
            dto.setEncontrado(true);

            String title = item.path("title").asText("").trim();
            dto.setNombre(limpiarNombre(title, codigo));

            String brand = item.path("brand").asText("").trim();
            dto.setMarca(brand);

            String category = item.path("category").asText("").trim();
            dto.setCategoria(category);

            // Extraer primera imagen si hay
            String imagenUrl = "";
            JsonNode images = item.path("images");
            if (images.isArray() && !images.isEmpty()) {
                imagenUrl = images.get(0).asText("").trim();
            }
            dto.setImagenUrl(imagenUrl);

            // Descargar imagen localmente si existe
            if (!imagenUrl.isEmpty()) {
                String rutaLocal = descargarImagenLocal(imagenUrl, codigo);
                dto.setImagenRuta(rutaLocal);
            }

            return dto;

        } catch (Exception e) {
            log.warn("[GLOBAL-SCAN] Excepción al consultar UPCitemdb para {}: {}", codigo, e.getMessage());
            return null;
        }
    }
}
