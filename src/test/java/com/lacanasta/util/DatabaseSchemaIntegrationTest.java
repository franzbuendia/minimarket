package com.lacanasta.util;

import com.lacanasta.dao.jdbc.CategoriaDAOJdbc;
import com.lacanasta.dao.jdbc.ConfigDAOJdbc;
import com.lacanasta.dao.jdbc.ProductoDAOJdbc;
import com.lacanasta.dao.jdbc.ProveedorDAOJdbc;
import com.lacanasta.dao.jdbc.UsuarioDAOJdbc;
import com.lacanasta.model.Categoria;
import com.lacanasta.model.EmpresaConfig;
import com.lacanasta.model.Producto;
import com.lacanasta.model.Proveedor;
import com.lacanasta.model.Usuario;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Prueba de integración real contra la base de datos MySQL reconstruida.
 */
class DatabaseSchemaIntegrationTest {

    @Test
    void verificarTablasYEntidades() throws Exception {
        try (Connection conn = new DatabaseConnection().getConnection()) {
            assertNotNull(conn, "La conexión a la BD minimarket debe ser exitosa");

            // 1. Usuarios
            UsuarioDAOJdbc usuarioDAO = new UsuarioDAOJdbc();
            List<Usuario> usuarios = usuarioDAO.findAll(conn);
            assertEquals(2, usuarios.size(), "Deben existir 2 usuarios");
            Usuario admin = usuarioDAO.findByUsername(conn, "admin");
            assertNotNull(admin, "Debe existir el usuario admin");

            // 2. Categorías
            CategoriaDAOJdbc categoriaDAO = new CategoriaDAOJdbc();
            List<Categoria> categorias = categoriaDAO.findAll(conn);
            assertTrue(categorias.size() >= 4, "Deben existir al menos 4 categorías");

            // 3. Empresa Configuración
            ConfigDAOJdbc configDAO = new ConfigDAOJdbc();
            EmpresaConfig config = configDAO.find(conn);
            assertNotNull(config, "Debe existir la configuración de la empresa");
            assertEquals("La Canasta", config.getNombreComercial());

            // 4. Proveedores
            ProveedorDAOJdbc proveedorDAO = new ProveedorDAOJdbc();
            List<Proveedor> proveedores = proveedorDAO.findAll(conn);
            assertEquals(2, proveedores.size(), "Deben existir 2 proveedores");

            // 5. Productos importados
            ProductoDAOJdbc productoDAO = new ProductoDAOJdbc();
            List<Producto> productos = productoDAO.findAll(conn);
            assertTrue(productos.size() >= 90, "Deben haberse cargado al menos 90 productos");

            Producto p1 = productoDAO.findById(conn, 1);
            assertNotNull(p1, "El producto 1 debe existir");
            assertEquals("Coca-Cola 1.5L", p1.getNombre());
            assertEquals("7891000012345", p1.getCodigoBarras());

            Producto p90 = productoDAO.findById(conn, 90);
            assertNotNull(p90, "El producto 90 debe existir");
            assertEquals("CEREAL BAR GOLDEN PAQ 21 GR", p90.getNombre());

            System.out.println("VERIFICACIÓN EXITOSA:");
            System.out.println("- Usuarios cargados: " + usuarios.size());
            System.out.println("- Categorías cargadas: " + categorias.size());
            System.out.println("- Proveedores cargados: " + proveedores.size());
            System.out.println("- Productos importados: " + productos.size());
            System.out.println("- Primer producto: " + p1.getNombre() + " (Stock: " + p1.getStock() + ")");
            System.out.println("- Último producto: " + p90.getNombre() + " (Stock: " + p90.getStock() + ")");
        }
    }
}
