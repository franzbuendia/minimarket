package com.lacanasta.controller;

import com.lacanasta.model.Caja;
import com.lacanasta.model.EstadoCaja;
import com.lacanasta.model.Producto;
import com.lacanasta.model.Rol;
import com.lacanasta.model.Usuario;
import com.lacanasta.service.CajaService;
import com.lacanasta.service.ConfigService;
import com.lacanasta.service.ProductoService;
import com.lacanasta.service.VentaService;
import com.lacanasta.util.SessionManager;
import javafx.application.Platform;
import javafx.scene.control.ListView;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.testfx.framework.junit5.ApplicationExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith({ApplicationExtension.class, MockitoExtension.class})
public class VentaScannerTest {

    @Mock
    private ProductoService productoService;

    @Mock
    private VentaService ventaService;

    @Mock
    private CajaService cajaService;

    @Mock
    private ConfigService configService;

    private VentaController controller;
    private Producto prod1;

    @BeforeAll
    static void initSession() {
        Usuario u = new Usuario();
        u.setIdUsuario(1);
        u.setNombre("Cajero de Prueba");
        u.setRol(Rol.CAJERO);
        SessionManager.setUsuarioActual(u);
    }

    @BeforeEach
    void setUp() throws Exception {
        controller = new VentaController(productoService, ventaService, cajaService, configService);

        prod1 = new Producto();
        prod1.setIdProducto(100);
        prod1.setCodigoBarras("7891000012345");
        prod1.setNombre("Coca Cola 1.5L");
        prod1.setPrecioNormal(new BigDecimal("4.50"));
        prod1.setStock(2);
        prod1.setEstado(true);

        // Inyectar campos FXML mediante reflexión
        setField(controller, "txtBuscar", new TextField());
        setField(controller, "txtMontoRecibido", new TextField());
        setField(controller, "lstCarrito", new ListView<>());
        setField(controller, "tablaResultados", new TableView<>());
        setField(controller, "lblTotal", new javafx.scene.control.Label());
        setField(controller, "lblVuelto", new javafx.scene.control.Label());
        setField(controller, "lblUnidades", new javafx.scene.control.Label());
        setField(controller, "btnPagar", new javafx.scene.control.Button());
    }

    private void setField(Object target, String fieldName, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(target, value);
    }

    @Test
    void testEscanearProductoExitoso() {
        when(productoService.findByCodigoBarras("7891000012345")).thenReturn(prod1);
        when(productoService.findById(100)).thenReturn(prod1);

        controller.procesarCodigo("7891000012345");

        // Validar que se agregó al carrito
        assertEquals(1, getCarritoSize());
    }

    @Test
    void testEscanearProductoDosVecesIncrementaCantidad() {
        when(productoService.findByCodigoBarras("7891000012345")).thenReturn(prod1);
        when(productoService.findById(100)).thenReturn(prod1);

        controller.procesarCodigo("7891000012345");
        controller.procesarCodigo("7891000012345");

        assertEquals(1, getCarritoSize());
        // Cantidad acumulada debe ser 2 (igual al stock máximo)
        assertEquals(2, getCarritoItemCantidad(0));
    }

    @Test
    void testEscanearProductoSuperaStockNoPermiteAgregar() {
        when(productoService.findByCodigoBarras("7891000012345")).thenReturn(prod1);
        when(productoService.findById(100)).thenReturn(prod1);

        // El stock es 2
        controller.procesarCodigo("7891000012345");
        controller.procesarCodigo("7891000012345");
        // Tercer escaneo debe ser rechazado por stock insuficiente
        controller.procesarCodigo("7891000012345");

        assertEquals(2, getCarritoItemCantidad(0));
    }

    @Test
    void testEscanearCodigoNoExistente() {
        when(productoService.findByCodigoBarras("9999999999999")).thenReturn(null);

        controller.procesarCodigo("9999999999999");

        assertEquals(0, getCarritoSize());
    }

    @SuppressWarnings("unchecked")
    private int getCarritoSize() {
        try {
            Field f = controller.getClass().getDeclaredField("carrito");
            f.setAccessible(true);
            List<?> list = (List<?>) f.get(controller);
            return list.size();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    private int getCarritoItemCantidad(int index) {
        try {
            Field f = controller.getClass().getDeclaredField("carrito");
            f.setAccessible(true);
            List<?> list = (List<?>) f.get(controller);
            Object item = list.get(index);
            Field cantField = item.getClass().getDeclaredField("cantidad");
            cantField.setAccessible(true);
            return (int) cantField.get(item);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
