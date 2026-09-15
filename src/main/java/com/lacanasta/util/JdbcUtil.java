package com.lacanasta.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;

/**
 * Utilidades de bajo nivel para mapear valores entre JDBC y las entidades del modelo.
 */
public final class JdbcUtil {

    private JdbcUtil() {
    }

    /**
     * Lee una fecha/hora de una columna del {@link ResultSet} como {@link LocalDateTime}.
     *
     * @param rs     el result set actual.
     * @param column el nombre de la columna.
     * @return el valor como LocalDateTime, o {@code null} si la columna es NULL.
     * @throws SQLException si ocurre un error de acceso.
     */
    public static LocalDateTime getLocalDateTime(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toLocalDateTime();
    }

    /**
     * Lee un entero nullable de una columna del {@link ResultSet}.
     *
     * @param rs     el result set actual.
     * @param column el nombre de la columna.
     * @return el valor como Integer, o {@code null} si la columna es NULL.
     * @throws SQLException si ocurre un error de acceso.
     */
    public static Integer getInteger(ResultSet rs, String column) throws SQLException {
        int value = rs.getInt(column);
        return rs.wasNull() ? null : value;
    }

    /**
     * Asigna un {@link LocalDateTime} a un parámetro del {@link PreparedStatement}, soportando null.
     *
     * @param ps    el statement preparado.
     * @param index posición del parámetro (1-based).
     * @param value valor a asignar (puede ser null).
     * @throws SQLException si ocurre un error al asignar.
     */
    public static void setLocalDateTime(PreparedStatement ps, int index, LocalDateTime value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.TIMESTAMP);
        } else {
            ps.setTimestamp(index, Timestamp.valueOf(value));
        }
    }

    /**
     * Asigna un {@link Integer} nullable a un parámetro del {@link PreparedStatement}.
     *
     * @param ps    el statement preparado.
     * @param index posición del parámetro (1-based).
     * @param value valor a asignar (puede ser null).
     * @throws SQLException si ocurre un error al asignar.
     */
    public static void setInteger(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }
}
