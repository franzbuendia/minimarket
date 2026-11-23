package com.lacanasta.util;

import javafx.scene.Node;
import javafx.scene.paint.Color;
import org.kordamp.ikonli.javafx.FontIcon;

/**
 * Generador centralizado de iconos vectoriales para toda la interfaz,
 * utilizando la biblioteca FontAwesome 5 (y Bootstrap / Feather) con tolerancia a fallos.
 */
public final class IconFactory {

    private IconFactory() {
    }

    /**
     * Crea un icono con el color y tamaño especificados.
     * Si el nombre no existe, recurre de forma segura a un icono genérico sin fallar.
     *
     * @param name  nombre lógico (ej. "sales", "cash") o literal directo ("fas-shopping-cart", "bi-cart").
     * @param size  tamaño en píxeles.
     * @param color color del icono.
     * @return el nodo visual FontIcon.
     */
    public static Node create(String name, double size, Color color) {
        String literal = resolveLiteral(name);
        FontIcon icon;
        try {
            icon = new FontIcon(literal);
        } catch (Exception e) {
            try {
                icon = new FontIcon("fas-circle");
            } catch (Exception ex) {
                try {
                    icon = new FontIcon("bi-circle");
                } catch (Exception ex2) {
                    return new javafx.scene.layout.Region();
                }
            }
        }
        icon.setIconSize((int) Math.round(size));
        if (color != null) {
            icon.setIconColor(color);
        }
        return icon;
    }

    /**
     * Resuelve el nombre lógico del icono a identificadores de alta fidelidad FontAwesome 5.
     */
    private static String resolveLiteral(String name) {
        if (name == null || name.isBlank()) {
            return "fas-circle";
        }
        if (name.startsWith("fas-") || name.startsWith("far-") || name.startsWith("fab-") ||
            name.startsWith("fea-") || name.startsWith("bi-")) {
            return name;
        }
        return switch (name.toLowerCase()) {
            case "sales", "cart" -> "fas-shopping-cart";
            case "cash", "caja" -> "fas-cash-register";
            case "reports", "stats" -> "fas-chart-line";
            case "products", "package" -> "fas-boxes";
            case "purchases", "basket" -> "fas-shopping-basket";
            case "suppliers", "truck" -> "fas-truck";
            case "users", "team" -> "fas-users-cog";
            case "settings", "gear" -> "fas-cog";
            case "logout" -> "fas-sign-out-alt";
            case "search" -> "fas-search";
            case "plus", "add" -> "fas-plus";
            case "minus" -> "fas-minus";
            case "trash", "delete" -> "fas-trash-alt";
            case "trend" -> "fas-chart-line";
            case "receipt", "ticket" -> "fas-receipt";
            case "check" -> "fas-check-circle";
            case "eye" -> "fas-eye";
            case "eye-slash" -> "fas-eye-slash";
            case "lock" -> "fas-lock";
            case "key" -> "fas-key";
            case "user" -> "fas-user";
            case "money", "cash-stack" -> "fas-money-bill-wave";
            case "credit-card", "tarjeta" -> "fas-credit-card";
            case "mobile", "phone", "yape" -> "fas-mobile-alt";
            case "store", "shop" -> "fas-store";
            case "server", "database" -> "fas-server";
            case "shield" -> "fas-shield-alt";
            case "calendar" -> "fas-calendar-alt";
            case "refresh" -> "fas-sync-alt";
            default -> "fas-circle";
        };
    }
}
