package com.lacanasta.controller;

import com.lacanasta.model.Rol;
import com.lacanasta.model.Usuario;
import com.lacanasta.util.SessionManager;
import com.lacanasta.util.ViewLoader;
import javafx.scene.Parent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;

import java.time.LocalDateTime;

@ExtendWith(ApplicationExtension.class)
public class AllViewsLoadTest {

    @BeforeAll
    static void setup() {
        Usuario testUser = new Usuario();
        testUser.setIdUsuario(1);
        testUser.setNombre("Administrador");
        testUser.setUsuario("admin");
        testUser.setRol(Rol.ADMIN);
        testUser.setEstado(true);
        testUser.setFechaCreacion(LocalDateTime.now());
        SessionManager.setUsuarioActual(testUser);
    }

    @Test
    void testLoadAllViews() throws Exception {
        String[] views = {
            "/com/lacanasta/fxml/login.fxml",
            "/com/lacanasta/fxml/main-view.fxml",
            "/com/lacanasta/fxml/ventas.fxml",
            "/com/lacanasta/fxml/caja.fxml",
            "/com/lacanasta/fxml/reportes.fxml",
            "/com/lacanasta/fxml/productos.fxml",
            "/com/lacanasta/fxml/compras.fxml",
            "/com/lacanasta/fxml/proveedores.fxml",
            "/com/lacanasta/fxml/usuarios.fxml",
            "/com/lacanasta/fxml/configuracion.fxml",
            "/com/lacanasta/fxml/producto-form.fxml",
            "/com/lacanasta/fxml/proveedor-form.fxml",
            "/com/lacanasta/fxml/usuario-form.fxml"
        };

        for (String view : views) {
            System.out.println("TESTING LOAD: " + view);
            try {
                Parent p = ViewLoader.load(view);
                System.out.println("SUCCESS: " + view + " -> " + p.getClass().getName());
            } catch (Throwable t) {
                System.err.println("FAILED TO LOAD " + view + ": " + t.getMessage());
                t.printStackTrace();
                throw t;
            }
        }
    }
}
