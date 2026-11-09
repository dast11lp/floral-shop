package com.floralshop.backend.catalog.infra.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import com.floralshop.backend.TestcontainersConfiguration;

// Integración de punta a punta: petición HTTP -> controlador -> servicio -> adapter -> Postgres real.
// Sin @Transactional: la transacción de lectura debe abrirla el adapter, como en producción.
// Los datos se guardan de verdad, y las tablas del catálogo se vacían antes y después de cada prueba.
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class CatalogApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JdbcTemplate jdbc;

    @BeforeEach
    @AfterEach
    void clean() {
        // En orden inverso a las claves foráneas.
        jdbc.update("delete from product_category");
        jdbc.update("delete from product_variant");
        jdbc.update("delete from product");
        jdbc.update("delete from category");
    }

    // ---- Datos de prueba, insertados directo por JDBC ----

    private long category(String name, String slug, int sortOrder) {
        return jdbc.queryForObject(
                "insert into category (name, slug, active, sort_order) values (?, ?, true, ?) returning id",
                Long.class, name, slug, sortOrder);
    }

    private long product(String name, String slug, boolean active) {
        return jdbc.queryForObject(
                "insert into product (name, slug, active) values (?, ?, ?) returning id",
                Long.class, name, slug, active);
    }

    private void variant(long productId, String name, String price, boolean active, int sortOrder) {
        jdbc.update("insert into product_variant (product_id, name, price, active, sort_order) values (?, ?, ?, ?, ?)",
                productId, name, new BigDecimal(price), active, sortOrder);
    }

    private void link(long productId, long categoryId) {
        jdbc.update("insert into product_category (product_id, category_id) values (?, ?)", productId, categoryId);
    }

    // ---- Pruebas ----

    @Test
    void emptyCatalogReturns200WithEmptyPage() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    @Test
    void listsProductsWithTheirActiveVariantsAndCategories() throws Exception {
        long id = product("Ramo primavera", "ramo-primavera", true);
        variant(id, "Mediano", "80000", true, 2);
        variant(id, "Pequeño", "50000", true, 1);
        variant(id, "Grande", "120000", false, 3); // inactiva: no debe salir
        link(id, category("Rosas", "rosas", 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ramo primavera"))
                .andExpect(jsonPath("$.content[0].variants.length()").value(2))
                .andExpect(jsonPath("$.content[0].variants[0].name").value("Pequeño")) // ordenadas por sort_order
                .andExpect(jsonPath("$.content[0].variants[0].price").value(50000.0))
                .andExpect(jsonPath("$.content[0].variants[1].name").value("Mediano"))
                .andExpect(jsonPath("$.content[0].categories[0].slug").value("rosas"))
                .andExpect(jsonPath("$.content[0].id").doesNotExist());
    }

    @Test
    void filtersByCategory() throws Exception {
        long rosas = category("Rosas", "rosas", 1);
        long tulipanes = category("Tulipanes", "tulipanes", 2);
        link(product("Ramo de rosas", "ramo-rosas", true), rosas);
        link(product("Ramo de tulipanes", "ramo-tulipanes", true), tulipanes);

        mockMvc.perform(get("/api/v1/products?category=rosas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].slug").value("ramo-rosas"));
    }

    @Test
    void paginatesAndReportsTotals() throws Exception {
        for (int i = 1; i <= 5; i++) {
            product("Producto " + i, "producto-" + i, true);
        }

        mockMvc.perform(get("/api/v1/products?page=2&size=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1)) // la última página trae solo 1
                .andExpect(jsonPath("$.content[0].name").value("Producto 5"))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.totalElements").value(5))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getsActiveProductBySlugButNotAnInactiveOne() throws Exception {
        product("Ramo visible", "ramo-visible", true);
        product("Ramo oculto", "ramo-oculto", false);

        mockMvc.perform(get("/api/v1/products/ramo-visible"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ramo visible"));

        mockMvc.perform(get("/api/v1/products/ramo-oculto"))
                .andExpect(status().isNotFound());
    }

    @Test
    void listsCategoriesInOrder() throws Exception {
        category("Alfa", "alfa", 2);
        category("Zeta", "zeta", 1);

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Zeta"))
                .andExpect(jsonPath("$[1].name").value("Alfa"));
    }
}