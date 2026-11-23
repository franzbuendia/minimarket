package com.lacanasta.service;

import com.lacanasta.dto.ProductoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link ProductoGlobalService}.
 */
class ProductoGlobalServiceTest {

    private ProductoGlobalService service;

    @BeforeEach
    void setUp() {
        service = new ProductoGlobalService();
    }

    @Test
    void testValidacionLongitudCodigoBarras() {
        // EAN-8 (8 dígitos) -> válido
        assertTrue(service.esCodigoValido("12345678"));

        // UPC-A (12 dígitos) -> válido
        assertTrue(service.esCodigoValido("012345678905"));

        // EAN-13 (13 dígitos) -> válido
        assertTrue(service.esCodigoValido("7750182001289"));
        assertTrue(service.esCodigoValido("6928175621285"));
        assertTrue(service.esCodigoValido("3017620422003"));
        assertTrue(service.esCodigoValido("5449000000996"));

        // ITF-14 / GTIN-14 (14 dígitos) -> válido
        assertTrue(service.esCodigoValido("12345678901234"));

        // Menos de 8 dígitos -> inválido
        assertFalse(service.esCodigoValido("1234567"));
        assertFalse(service.esCodigoValido("123"));

        // Más de 14 dígitos -> inválido
        assertFalse(service.esCodigoValido("123456789012345"));

        // No numérico o nulo -> inválido
        assertFalse(service.esCodigoValido("775018200128A"));
        assertFalse(service.esCodigoValido(""));
        assertFalse(service.esCodigoValido("   "));
        assertFalse(service.esCodigoValido(null));
    }

    @Test
    void testLimpiarNombreProducto() {
        // 1. Elimina sufijo " X" o " x"
        assertEquals("Alacena Uchucuta Crema Rocoto",
                ProductoGlobalService.limpiarNombreProducto("Alacena Uchucuta Crema Rocoto X"));
        assertEquals("Alacena Uchucuta Crema Rocoto",
                ProductoGlobalService.limpiarNombreProducto("Alacena Uchucuta Crema Rocoto x"));

        // 2. Elimina sufijo "(importado)"
        assertEquals("Nutella",
                ProductoGlobalService.limpiarNombreProducto("Nutella (importado)"));
        assertEquals("Nutella",
                ProductoGlobalService.limpiarNombreProducto("Nutella (import)"));

        // 3. Normaliza espacios múltiples
        assertEquals("Galletas Oreo Chocolate",
                ProductoGlobalService.limpiarNombreProducto("  Galletas   Oreo    Chocolate  "));

        // 4. Quita caracteres sueltos o puntuación al final
        assertEquals("Coca Cola 500ml",
                ProductoGlobalService.limpiarNombreProducto("Coca Cola 500ml - "));
        assertEquals("Fideos Don Vittorio",
                ProductoGlobalService.limpiarNombreProducto("Fideos Don Vittorio /"));

        // 5. Trunca a 100 caracteres máximo
        String textoLargo = "P".repeat(150);
        String limpio = ProductoGlobalService.limpiarNombreProducto(textoLargo);
        assertEquals(100, limpio.length());
    }

    @Test
    void testLimpiarNombreConRespaldoCodigo() {
        assertEquals("Producto 3017620422003",
                ProductoGlobalService.limpiarNombre("   ", "3017620422003"));
        assertEquals("Producto sin nombre",
                ProductoGlobalService.limpiarNombre(null, null));
    }

    @Test
    void testConsultarProductoNutellaGlobal() {
        // 3017620422003 -> Francés (Nutella)
        ProductoDTO dto = service.buscarGlobal("3017620422003");
        assertNotNull(dto);
        if (dto.isEncontrado()) {
            assertNotNull(dto.getNombre());
            assertTrue(dto.getNombre().toLowerCase().contains("nutella"));
            assertFalse(dto.getNombre().endsWith(" X"));
        }
    }

    @Test
    void testConsultarProductoCocaColaGlobal() {
        // 5449000000996 -> Belga/Global (Coca-Cola)
        ProductoDTO dto = service.buscarGlobal("5449000000996");
        assertNotNull(dto);
        if (dto.isEncontrado()) {
            assertNotNull(dto.getNombre());
            assertTrue(dto.getNombre().toLowerCase().contains("coca"));
        }
    }

    @Test
    void testDescargarImagenLocalConUrlValidaYFallosControlados() {
        // Fallos controlados sin excepciones
        assertNull(service.descargarImagenLocal(null, "12345678"));
        assertNull(service.descargarImagenLocal("", "12345678"));
        assertNull(service.descargarImagenLocal("https://dominio-inexistente-12345.org/foto.jpg", "12345678"));
    }

    @Test
    void testCodigoInvalidoNoConsultaApi() {
        ProductoDTO dto = service.buscarGlobal("123");
        assertNotNull(dto);
        assertFalse(dto.isEncontrado());
    }
}
