package com.lacanasta.service;

import com.lacanasta.dto.ProductoDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pruebas unitarias para {@link ProductoPeruanoService} y la Regla de Oro 🇵🇪.
 */
class ProductoPeruanoServiceTest {

    private ProductoPeruanoService service;

    @BeforeEach
    void setUp() {
        service = new ProductoPeruanoService();
    }

    @Test
    void testPrefijoPeruano775EsValido() {
        assertTrue(service.esPrefijoPeruano("7750182001289"));
        assertTrue(service.esPrefijoPeruano("7750727014233"));
        assertTrue(service.esPrefijoPeruano("7759999999999"));
    }

    @Test
    void testPrefijoPeruano779EsValido() {
        assertTrue(service.esPrefijoPeruano("7799999999999"));
        assertTrue(service.esPrefijoPeruano("7791234567890"));
    }

    @Test
    void testPrefijosExtranjerosSonRechazadosSinConsultarApi() {
        // 692... (China)
        assertFalse(service.esPrefijoPeruano("6928175621285"));
        ProductoDTO dtoChino = service.buscarEnPeru("6928175621285");
        assertNotNull(dtoChino);
        assertFalse(dtoChino.isPrefijoPeruano());
        assertFalse(dtoChino.isEncontrado());

        // 750... (México)
        assertFalse(service.esPrefijoPeruano("7501005555555"));
        ProductoDTO dtoMex = service.buscarEnPeru("7501005555555");
        assertNotNull(dtoMex);
        assertFalse(dtoMex.isPrefijoPeruano());
        assertFalse(dtoMex.isEncontrado());
    }

    @Test
    void testCodigoInexistenteEmpieza775RetornaNoEncontrado() {
        ProductoDTO dto = service.buscarEnPeru("7759999999999");
        assertNotNull(dto);
        assertTrue(dto.isPrefijoPeruano());
        assertFalse(dto.isEncontrado());
    }

    @Test
    void testCodigoInexistenteEmpieza779RetornaNoEncontrado() {
        ProductoDTO dto = service.buscarEnPeru("7799999999999");
        assertNotNull(dto);
        assertTrue(dto.isPrefijoPeruano());
        assertFalse(dto.isEncontrado());
    }

    @Test
    void testLimpiarNombreEliminaSufijoX() {
        assertEquals("Alacena Uchucuta Crema Rocoto",
                ProductoPeruanoService.limpiarNombre("Alacena Uchucuta Crema Rocoto X", "7750243040327"));
        assertEquals("Alacena Uchucuta Crema Rocoto",
                ProductoPeruanoService.limpiarNombre("Alacena Uchucuta Crema Rocoto x", "7750243040327"));
        assertEquals("Alacena Uchucuta Crema Rocoto",
                ProductoPeruanoService.limpiarNombre("Alacena Uchucuta Crema Rocoto X -", "7750243040327"));
        assertEquals("Inca Kola 500ml",
                ProductoPeruanoService.limpiarNombre("  Inca Kola 500ml  ", "7750182006088"));
    }

    @Test
    void testLimpiarNombreRespaldoSiVacioOExtenso() {
        assertEquals("Producto 7750243040327",
                ProductoPeruanoService.limpiarNombre("   ", "7750243040327"));
        assertEquals("Producto sin nombre",
                ProductoPeruanoService.limpiarNombre("", null));

        String muyLargo = "A".repeat(150);
        String limpio = ProductoPeruanoService.limpiarNombre(muyLargo, "12345");
        assertTrue(limpio.length() <= 100);
    }

    @Test
    void testBuscarAlacenaRocotoLimpiaNombreSinX() {
        // Barcode del caso reportado por el usuario: 7750243040327
        assertTrue(service.esPrefijoPeruano("7750243040327"));
        ProductoDTO dto = service.buscarEnPeru("7750243040327");
        assertNotNull(dto);
        assertTrue(dto.isPrefijoPeruano());
        if (dto.isEncontrado()) {
            assertNotNull(dto.getNombre());
            assertFalse(dto.getNombre().endsWith(" X"), "El nombre no debe terminar en ' X'");
            assertFalse(dto.getNombre().endsWith(" x"), "El nombre no debe terminar en ' x'");
        }
    }

    @Test
    void testCodigoPeruanoRealEnOpenFoodFacts() {
        // Inca Kola (7750182006088) registrado en Open Food Facts
        ProductoDTO dto = service.buscarEnPeru("7750182006088");
        assertNotNull(dto);
        assertTrue(dto.isPrefijoPeruano());
        if (dto.isEncontrado()) {
            assertTrue(dto.isVendidoEnPeru());
            assertNotNull(dto.getNombre());
        }
    }
}
