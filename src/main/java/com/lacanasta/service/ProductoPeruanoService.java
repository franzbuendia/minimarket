package com.lacanasta.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lacanasta.dto.ProductoDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Servicio para la identificación automática de productos peruanos
 * basándose en la Regla de Oro (prefijos GS1 775/779 y validación de comercialización en Perú).
 */
public class ProductoPeruanoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoPeruanoService.class);
    private static final String API_URL_TEMPLATE = "https://world.openfoodfacts.org/api/v2/product/%s.json";
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public ProductoPeruanoService() {
        this(HttpClient.newBuilder()
                .connectTimeout(TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(), new ObjectMapper());
    }

    public ProductoPeruanoService(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Limpia el nombre del producto eliminando sufijos o artefactos comunes
     * (por ejemplo, la terminación ' X' o ' x', caracteres de puntuación al final),
     * normaliza espacios y trunca a un máximo de 100 caracteres para MySQL.
     *
     * @param nombre nombre comercial en bruto.
     * @param codigo código de barras para respaldo si queda vacío.
     * @return nombre limpio y validado para la base de datos.
     */
    public static String limpiarNombre(String nombre, String codigo) {
        if (nombre == null) {
            return (codigo != null && !codigo.isBlank()) ? "Producto " + codigo.trim() : "Producto sin nombre";
        }
        String limpio = nombre.trim();
        // Unificar espacios en blanco consecutivos
        limpio = limpio.replaceAll("\\s+", " ");
        // Quitar artefactos finales comunes como ' X' o ' x' aislados (case-insensitive)
        limpio = limpio.replaceAll("(?i)\\s+[xX]$", "").trim();
        // Quitar caracteres no deseados al final como '-', '_', ',', '/', '.'
        limpio = limpio.replaceAll("[\\-_,/.\\s]+$", "").trim();
        // Volver a verificar si quedó un ' X' o ' x' tras remover puntuación
        limpio = limpio.replaceAll("(?i)\\s+[xX]$", "").trim();

        if (limpio.isEmpty()) {
            limpio = (codigo != null && !codigo.isBlank()) ? "Producto " + codigo.trim() : "Producto sin nombre";
        }
        // Restricción de base de datos VARCHAR(100)
        if (limpio.length() > 100) {
            limpio = limpio.substring(0, 100).trim();
        }
        return limpio;
    }

    /**
     * Evalúa si un código de barras corresponde al estándar GS1 asignado a Perú (prefijos 775 o 779).
     *
     * @param codigo código de barras numérico.
     * @return {@code true} si inicia con 775 o 779, {@code false} en caso contrario.
     */
    public boolean esPrefijoPeruano(String codigo) {
        if (codigo == null) {
            return false;
        }
        String limpio = codigo.trim();
        return limpio.startsWith("775") || limpio.startsWith("779");
    }

    /**
     * Consulta bases de datos externas peruanas (Open Food Facts con filtro de país Perú).
     * REGLA DE ORO: Si el código NO empieza con 775 ni 779, NO realiza ninguna petición HTTP.
     *
     * @param codigoBarra código escaneado.
     * @return ProductoDTO con el diagnóstico y metadatos encontrados.
     */
    public ProductoDTO buscarEnPeru(String codigoBarra) {
        ProductoDTO dto = new ProductoDTO(codigoBarra);
        if (codigoBarra == null || codigoBarra.trim().isEmpty()) {
            dto.setPrefijoPeruano(false);
            dto.setEncontrado(false);
            dto.setVendidoEnPeru(false);
            return dto;
        }

        String codigo = codigoBarra.trim();
        log.info("[PERU-API] Validando código de barras: {}", codigo);

        // REGLA DE ORO 1: Validar prefijo 775 / 779 antes de consultar cualquier API
        if (!esPrefijoPeruano(codigo)) {
            log.warn("[PERU-API] Código descartado (no peruano): {}", codigo);
            dto.setPrefijoPeruano(false);
            dto.setEncontrado(false);
            dto.setVendidoEnPeru(false);
            return dto;
        }

        dto.setPrefijoPeruano(true);
        String url = String.format(API_URL_TEMPLATE, codigo);
        log.info("[PERU-API] Prefijo peruano confirmado. Consultando API externa: {}", url);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(TIMEOUT)
                    .header("User-Agent", "LaCanastaPOS/1.0 (contacto@lacanasta.pe)")
                    .header("Accept", "application/json")
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 404) {
                log.info("[PERU-API] Código {} no encontrado en base externa (HTTP 404)", codigo);
                dto.setEncontrado(false);
                dto.setVendidoEnPeru(false);
                return dto;
            }

            if (response.statusCode() != 200) {
                log.warn("[PERU-API] Código {} retornó estado HTTP no exitoso: {}", codigo, response.statusCode());
                dto.setEncontrado(false);
                dto.setVendidoEnPeru(false);
                return dto;
            }

            JsonNode root = objectMapper.readTree(response.body());
            int status = root.path("status").asInt(0);
            if (status != 1) {
                log.info("[PERU-API] El servicio respondió status=0 (producto {} no registrado en la base)", codigo);
                dto.setEncontrado(false);
                dto.setVendidoEnPeru(false);
                return dto;
            }

            dto.setEncontrado(true);
            JsonNode prodNode = root.path("product");

            // Nombre comercial
            String nombre = prodNode.path("product_name").asText("").trim();
            if (nombre.isEmpty()) {
                nombre = prodNode.path("product_name_es").asText("").trim();
            }
            if (nombre.isEmpty()) {
                nombre = prodNode.path("generic_name").asText("").trim();
            }
            dto.setNombre(limpiarNombre(nombre, codigo));

            // Marca
            String marca = prodNode.path("brands").asText("").trim();
            dto.setMarca(marca);

            // Categoría
            String categorias = prodNode.path("categories").asText("").trim();
            dto.setCategoria(categorias);

            // Foto del producto
            String imagen = prodNode.path("image_url").asText("").trim();
            if (imagen.isEmpty()) {
                imagen = prodNode.path("image_front_url").asText("").trim();
            }
            dto.setImagenUrl(imagen);

            // Países de venta
            String countries = prodNode.path("countries").asText("").trim();
            dto.setPaises(countries);

            // REGLA DE ORO 2: Validar si se comercializa en Perú
            boolean vendidoEnPeru = verificarVentaEnPeru(prodNode, countries);
            dto.setVendidoEnPeru(vendidoEnPeru);

            if (vendidoEnPeru) {
                log.info("[PERU-API] ÉXITO: Producto peruano verificado -> [{}] {} | Marca: {}",
                        codigo, dto.getNombre(), dto.getMarca());
            } else {
                log.warn("[PERU-API] Producto {} registrado pero no en Perú (Países: {})", codigo, countries);
            }

            return dto;

        } catch (Exception e) {
            log.error("[PERU-API] Error o tiempo de espera agotado para el código {}: {}", codigo, e.getMessage());
            dto.setEncontrado(false);
            dto.setVendidoEnPeru(false);
            return dto;
        }
    }

    private boolean verificarVentaEnPeru(JsonNode productNode, String countries) {
        if (countries != null && !countries.isBlank()) {
            String c = countries.toLowerCase();
            if (c.contains("peru") || c.contains("perú")) {
                return true;
            }
        }

        JsonNode countriesTags = productNode.path("countries_tags");
        if (countriesTags.isArray()) {
            for (JsonNode tag : countriesTags) {
                String t = tag.asText("").toLowerCase();
                if (t.equals("en:peru") || t.contains("peru")) {
                    return true;
                }
            }
        }

        JsonNode statesTags = productNode.path("states_tags");
        if (statesTags.isArray()) {
            for (JsonNode tag : statesTags) {
                String t = tag.asText("").toLowerCase();
                if (t.equals("en:peru") || t.contains("peru")) {
                    return true;
                }
            }
        }

        return false;
    }
}
