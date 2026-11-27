package com.lacanasta.util;

import com.lacanasta.util.ResourceMonitor.EstadoRecursos;
import com.lacanasta.util.ResourceMonitor.NivelEstado;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void iniciarYDetenerNoLanzaExcepcion() {
        ResourceMonitor.iniciar();
        ResourceMonitor.detener();
    }
}
