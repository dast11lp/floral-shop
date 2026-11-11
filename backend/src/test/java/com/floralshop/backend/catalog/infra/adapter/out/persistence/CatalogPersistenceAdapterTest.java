package com.floralshop.backend.catalog.infra.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

import com.floralshop.backend.TestcontainersConfiguration;
import com.floralshop.backend.catalog.application.port.out.CatalogRepositoryPort;
import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;
import com.floralshop.backend.catalog.domain.model.ProductVariant;
import com.floralshop.backend.catalog.domain.model.VariantWorkload;

import jakarta.persistence.EntityManagerFactory;

// Integración: Spring completo + Postgres real (Testcontainers).
// OJO: NO lleva @Transactional. Debe ser el adapter quien abra su transacción;
// si la prueba la abriera, ocultaría una transacción que faltara en el adapter.
// Como los datos se guardan de verdad, las tablas del catálogo se vacían antes y después de cada prueba.
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class CatalogPersistenceAdapterTest {

    @Autowired
    CatalogRepositoryPort catalog; // el PUERTO: probamos lo que usará el negocio

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    EntityManagerFactory emf;

    private Statistics statistics;

    @BeforeEach
    void setUp() {
        clean();
        statistics = emf.unwrap(SessionFactory.class).getStatistics();
        statistics.setStatisticsEnabled(true); // para contar consultas
    }

    @AfterEach
    void tearDown() {
        clean();
    }

    // En orden inverso a las claves foráneas.
    private void clean() {
        jdbc.update("delete from product_category");
        jdbc.update("delete from product_variant");
        jdbc.update("delete from product");
        jdbc.update("delete from category");
    }

    // ---- Datos de prueba, insertados directo por JDBC ----

    private long variantId(long productId, String name) {
        return jdbc.queryForObject("select id from product_variant where product_id = ? and name = ?",
                Long.class, productId, name);
    }

    @Test
    void workloadInheritsOrUsesItsOwnTime() {
        long ramo = product("Ramo", "ramo", true);
        variant(ramo, "Pequeño", "50000", true, 1);
        variant(ramo, "Boda", "300000", true, 2);
        jdbc.update("update product set crafting_hours = 4 where id = ?", ramo);
        jdbc.update("update product_variant set crafting_hours = 48, slot_units = 3 "
                + "where product_id = ? and name = 'Boda'", ramo);
        long small = variantId(ramo, "Pequeño");
        long wedding = variantId(ramo, "Boda");

        List<VariantWorkload> found = catalog.findAvailableWorkloads(List.of(small, wedding));

        assertThat(found).containsExactlyInAnyOrder(
                new VariantWorkload(small, null, 4, 1),      // hereda; 1 cupo por defecto
                new VariantWorkload(wedding, 48, 4, 3));
        assertThat(found).extracting(VariantWorkload::craftingHours).containsExactlyInAnyOrder(4, 48);
    }

    @Test
    void onlyRequestedVariantsCome() {
        long ramo = product("Ramo", "ramo", true);
        variant(ramo, "Pequeño", "50000", true, 1);
        variant(ramo, "Mediano", "70000", true, 2);

        assertThat(catalog.findAvailableWorkloads(List.of(variantId(ramo, "Mediano"))))
                .extracting(VariantWorkload::variantId)
                .containsExactly(variantId(ramo, "Mediano"));
    }

    @Test
    void inactiveVariantDoesNotCome() {
        long ramo = product("Ramo", "ramo", true);
        variant(ramo, "Retirado", "70000", false, 1);

        assertThat(catalog.findAvailableWorkloads(List.of(variantId(ramo, "Retirado")))).isEmpty();
    }

    @Test
    void variantOfInactiveProductDoesNotCome() {
        long oculto = product("Oculto", "oculto", false);
        variant(oculto, "Único", "50000", true, 1);

        assertThat(catalog.findAvailableWorkloads(List.of(variantId(oculto, "Único")))).isEmpty();
    }

    @Test
    void unknownIdsAndEmptyRequestReturnNothing() {
        assertThat(catalog.findAvailableWorkloads(List.of(999_999L))).isEmpty();
        assertThat(catalog.findAvailableWorkloads(List.of())).isEmpty();
    }

    private long category(String name, String slug, boolean active, int sortOrder) {
        return jdbc.queryForObject(
                "insert into category (name, slug, active, sort_order) values (?, ?, ?, ?) returning id",
                Long.class, name, slug, active, sortOrder);
    }

    private long product(String name, String slug, boolean active) {
        return jdbc.queryForObject(
                "insert into product (name, slug, active, crafting_hours) values (?, ?, ?, 24) returning id",
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
    void listsOnlyActiveProductsOrderedByName() {
        product("Tulipanes", "tulipanes", true);
        product("Azucenas", "azucenas", true);
        product("Oculto", "oculto", false);

        PagedResult<Product> result = catalog.findActiveProducts(new PageQuery(0, 10));

        assertThat(result.content()).extracting(Product::name).containsExactly("Azucenas", "Tulipanes");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void paginatesWithoutRepeatingOrSkipping() {
        for (int i = 1; i <= 5; i++) {
            product("Producto " + i, "producto-" + i, true);
        }

        PagedResult<Product> first = catalog.findActiveProducts(new PageQuery(0, 2));
        assertThat(first.totalElements()).isEqualTo(5);
        assertThat(first.totalPages()).isEqualTo(3);

        // Recorre las tres páginas: deben salir los 5, en orden, sin repetir ni saltar ninguno.
        List<String> names = new ArrayList<>();
        for (int page = 0; page < 3; page++) {
            catalog.findActiveProducts(new PageQuery(page, 2)).content()
                    .forEach(product -> names.add(product.name()));
        }
        assertThat(names).containsExactly(
                "Producto 1", "Producto 2", "Producto 3", "Producto 4", "Producto 5");
    }

    @Test
    void hidesInactiveVariantsAndCategoriesInsideAProduct() {
        long id = product("Ramo", "ramo", true);
        variant(id, "Pequeño", "50000", true, 1);
        variant(id, "Mediano", "80000", true, 2);
        variant(id, "Grande", "120000", false, 3); // inactiva: no debe salir
        long rosas = category("Rosas", "rosas", true, 1);
        long antiguas = category("Antiguas", "antiguas", false, 2); // inactiva: no debe salir
        link(id, rosas);
        link(id, antiguas);

        Product ramo = catalog.findActiveBySlug("ramo").orElseThrow();

        assertThat(ramo.variants()).extracting(ProductVariant::name).containsExactly("Pequeño", "Mediano");
        assertThat(ramo.variants().get(0).price()).isEqualByComparingTo("50000");
        assertThat(ramo.categories()).extracting(Category::name).containsExactly("Rosas");
    }

    @Test
    void ordersVariantsBySortOrderAndCategoriesBySortOrderThenName() {
        long id = product("Ramo", "ramo", true);
        // Insertadas desordenadas a propósito.
        variant(id, "Grande", "120000", true, 3);
        variant(id, "Pequeño", "50000", true, 1);
        variant(id, "Mediano", "80000", true, 2);
        link(id, category("Zeta", "zeta", true, 1));
        link(id, category("Beta", "beta", true, 2));
        link(id, category("Alfa", "alfa", true, 2)); // empata con Beta en sortOrder: desempata el nombre

        Product ramo = catalog.findActiveBySlug("ramo").orElseThrow();

        assertThat(ramo.variants()).extracting(ProductVariant::name)
                .containsExactly("Pequeño", "Mediano", "Grande");
        assertThat(ramo.categories()).extracting(Category::name)
                .containsExactly("Zeta", "Alfa", "Beta");
    }

    @Test
    void filtersProductsByCategory() {
        long rosas = category("Rosas", "rosas", true, 1);
        long tulipanes = category("Tulipanes", "tulipanes", true, 2);
        long a = product("Ramo A", "ramo-a", true);
        long b = product("Ramo B", "ramo-b", true);
        long c = product("Ramo C", "ramo-c", true);
        long oculto = product("Ramo oculto", "ramo-oculto", false);
        link(a, rosas);
        link(b, rosas);
        link(oculto, rosas); // producto inactivo en una categoría activa: no debe salir
        link(c, tulipanes);

        PagedResult<Product> result = catalog.findActiveProductsByCategory("rosas", new PageQuery(0, 10));

        assertThat(result.content()).extracting(Product::name).containsExactly("Ramo A", "Ramo B");
        assertThat(result.totalElements()).isEqualTo(2);
    }

    @Test
    void inactiveCategoryReturnsNoProducts() {
        long antiguas = category("Antiguas", "antiguas", false, 1);
        link(product("Ramo", "ramo", true), antiguas);

        PagedResult<Product> result = catalog.findActiveProductsByCategory("antiguas", new PageQuery(0, 10));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void unknownCategoryReturnsEmptyPage() {
        product("Ramo", "ramo", true);

        PagedResult<Product> result = catalog.findActiveProductsByCategory("no-existe", new PageQuery(0, 10));

        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isZero();
    }

    @Test
    void findBySlugIgnoresInactiveAndMissingProducts() {
        product("Oculto", "oculto", false);

        assertThat(catalog.findActiveBySlug("oculto")).isEmpty();
        assertThat(catalog.findActiveBySlug("no-existe")).isEmpty();
    }

    @Test
    void listsOnlyActiveCategoriesOrdered() {
        category("Alfa", "alfa", true, 2);
        category("Zeta", "zeta", true, 1);
        category("Oculta", "oculta", false, 0);

        assertThat(catalog.findActiveCategories()).extracting(Category::name).containsExactly("Zeta", "Alfa");
    }

    @Test
    void queryCountDoesNotGrowWithPageSize() {
        long rosas = category("Rosas", "rosas", true, 1);
        long tulipanes = category("Tulipanes", "tulipanes", true, 2);
        for (int i = 1; i <= 20; i++) {
            long id = product("Producto " + i, "producto-" + i, true);
            variant(id, "Pequeño", "50000", true, 1);
            variant(id, "Grande", "90000", true, 2);
            link(id, rosas);
            link(id, tulipanes);
        }

        long queriesForOne = queriesFor(new PageQuery(0, 1));
        long queriesForTwenty = queriesFor(new PageQuery(0, 20));

        // Con el problema N+1, pedir 20 productos lanzaría muchas más consultas que pedir 1.
        assertThat(queriesForTwenty).isEqualTo(queriesForOne);
    }

    private long queriesFor(PageQuery page) {
        statistics.clear();
        catalog.findActiveProducts(page);
        return statistics.getPrepareStatementCount();
    }
}