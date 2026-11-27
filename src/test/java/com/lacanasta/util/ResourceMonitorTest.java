package com.lacanasta.util;

import com.lacanasta.util.ResourceMonitor.EstadoRecursos;
import com.lacanasta.util.ResourceMonitor.NivelEstado;
import com.lacanasta.util.ResourceMonitor.RegistroEvento;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResourceMonitorTest {

    @Test
    void capturarEstadoDevuelveMetricasValidas() {
        EstadoRecursos estado = ResourceMonitor.capturarEstado();
        assertNotNull(estado);
        assertTrue(estado.memoriaDatosMB() >= 0);
        assertTrue(estado.memoriaDatosMaxMB() > 0);
        assertTrue(estado.memoriaSistemaMB() >= 0);
        assertTrue(estado.procesadorPorcentaje() >= 0.0 && estado.procesadorPorcentaje() <= 100.0);
        assertTrue(estado.tareasSimultaneas() > 0);
        assertTrue(estado.componentesCargados() > 0);
        assertNotNull(estado.nivelGeneral());
    }

    @Test
    void evaluarNivelRespetaUmbralesAtencionYCritico() {
        assertEquals(NivelEstado.OPTIMO, ResourceMonitor.evaluarNivel(50, 250, 400));
        assertEquals(NivelEstado.ATENCION, ResourceMonitor.evaluarNivel(260, 250, 400));
        assertEquals(NivelEstado.CRITICO, ResourceMonitor.evaluarNivel(420, 250, 400));
    }

    @Test
    void registrarActividadAlmacenaYClasificaPorDuracion() {
        ResourceMonitor.limpiarHistorialEventos();
        ResourceMonitor.registrarActividad("Abrir Ventas", 85);
        ResourceMonitor.registrarActividad("Abrir Reportes", 620);

        List<RegistroEvento> eventos = ResourceMonitor.getHistorialEventos();
        assertEquals(2, eventos.size());
        assertEquals("Abrir Reportes", eventos.get(0).accion());
        assertEquals("620 ms", eventos.get(0).duracion());
        assertEquals(NivelEstado.ATENCION, eventos.get(0).estado());
        assertEquals("Abrir Ventas", eventos.get(1).accion());

        ResourceMonitor.limpiarHistorialEventos();
        assertTrue(ResourceMonitor.getHistorialEventos().isEmpty());
    }

    @Test
    void modoPruebaClonaYRegresaALaBaseRealSinAlterarla() throws Exception {
        assertFalse(DatabaseConnection.isModoPruebaActivo());
        String bdReal = DatabaseConnection.getBaseDatosActiva();
        try {
            DatabaseConnection.activarModoPrueba();
            assertTrue(DatabaseConnection.isModoPruebaActivo());
            assertEquals("minimarket_pruebas", DatabaseConnection.getBaseDatosActiva());
            assertTrue(ResourceMonitor.medirBaseDatosMs() >= 0);
        } finally {
            DatabaseConnection.desactivarModoPrueba();
        }
        assertFalse(DatabaseConnection.isModoPruebaActivo());
        assertEquals(bdReal, DatabaseConnection.getBaseDatosActiva());
    }

    @Test
    void iniciarYDetenerNoLanzaExcepcion() {
        ResourceMonitor.iniciar();
        ResourceMonitor.detener();
    }
}
