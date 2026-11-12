package com.lacanasta.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Vista unificada de un movimiento de caja: ventas en efectivo e ingresos/egresos
 * extraordinarios.
 *
 * @param fecha       fecha/hora del movimiento.
 * @param tipo        etiqueta legible: "Venta", "Ingreso" o "Egreso".
 * @param descripcion detalle del movimiento.
 * @param monto       monto con signo (positivo = entrada, negativo = salida).
 */
public record RegistroCaja(LocalDateTime fecha, String tipo, String descripcion, BigDecimal monto) {
}
