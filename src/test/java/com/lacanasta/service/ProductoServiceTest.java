package com.lacanasta.service;

import com.lacanasta.dao.ProductoDAO;
import com.lacanasta.model.Producto;
import com.lacanasta.util.ConnectionProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductoServiceTest {

    @Mock
    private ProductoDAO productoDAO;

    @Mock
    private ConnectionProvider connectionProvider;

    @Mock
    private Connection connection;

    private ProductoService productoService;

    @BeforeEach
    void setUp() throws SQLException {
        when(connectionProvider.getConnection()).thenReturn(connection);
        productoService = new ProductoService(productoDAO, connectionProvider);
    }

    @Test
    void testGuardarProductoNuevoExitoso() throws SQLException {
        Producto nuevo = new Producto();
        nuevo.setCodigoBarras("7750243040327");
        nuevo.setNombre("Alacena Uchucuta Crema Rocoto");
        nuevo.setPrecioNormal(new BigDecimal("3.00"));
        nuevo.setCosto(new BigDecimal("2.50"));
        nuevo.setStock(10);

        when(productoDAO.findByCodigoBarras(connection, "7750243040327")).thenReturn(null);
        when(productoDAO.insert(eq(connection), any(Producto.class))).thenReturn(95);

        productoService.save(nuevo);

        assertEquals(95, nuevo.getIdProducto());
        assertNotNull(nuevo.getFechaCreacion());
        verify(productoDAO).insert(connection, nuevo);
    }

    @Test
    void testGuardarProductoDuplicadoLanzaServiceException() throws SQLException {
        Producto nuevo = new Producto();
        nuevo.setCodigoBarras("7750243040327");
        nuevo.setNombre("Alacena Uchucuta Crema Rocoto");

        Producto existente = new Producto();
        existente.setIdProducto(92);
        existente.setCodigoBarras("7750243040327");

        when(productoDAO.findByCodigoBarras(connection, "7750243040327")).thenReturn(existente);

        ServiceException ex = assertThrows(ServiceException.class, () -> productoService.save(nuevo));
        assertTrue(ex.getMessage().contains("ya está registrado"));
    }

    @Test
    void testGuardarProductoConErrorSqlLanzaMensajeDetallado() throws SQLException {
        Producto nuevo = new Producto();
        nuevo.setCodigoBarras("7750243040327");
        nuevo.setNombre("Alacena Uchucuta Crema Rocoto");

        when(productoDAO.findByCodigoBarras(connection, "7750243040327")).thenReturn(null);
        when(productoDAO.insert(eq(connection), any(Producto.class)))
                .thenThrow(new SQLException("Duplicate entry '7750243040327' for key 'uq_productos_codigo_barras'"));

        ServiceException ex = assertThrows(ServiceException.class, () -> productoService.save(nuevo));
        assertTrue(ex.getMessage().contains("Duplicate entry"));
    }
}
