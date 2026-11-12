package com.floralshop.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest // significa: “monta tu aplicación Spring completa para esta prueba”""
@Import(TestcontainersConfiguration.class) // significa: “además de la aplicación, usa esta configuración extra”
class DatabaseMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    //valida que la migración de la base de datos haya creado todas las tablas necesarias
    @Test
    void migrationCreatesAllTables() {
        List<String> tables = jdbc.queryForList( // devuelve filas de la consulta SQL como una lista de objetos
                "select table_name from information_schema.tables where table_schema = 'public'",
                String.class); // devuelve una lista de nombres de tablas

        assertThat(tables).contains( // comprueba que la lista contiene todos los nombres de tablas esperados
                "category", "product", "product_category", "product_variant",
                "delivery_zone", "delivery_slot", "delivery_settings",
                "customer_order", "order_line", "order_settings")
                .doesNotContain("shop_settings"); // V5 la separó por módulo
    }

    // comprueba que la regla de que a capacidad de entrega no supere el número de pedidos en una franja horaria
    @Test
    void databaseRejectsMoreBookedThanCapacity() {
        assertThatThrownBy(() -> jdbc.update( // intenta insertar un registro que viola la restricción de capacidad, debe ser lambda ya que AssertJ necesita una función que lance la excepción para capturarla, que el la use cuando crea indicado y no se el escape la excepción
                "insert into delivery_slot (slot_date, start_time, end_time, capacity, booked) "
                        + "values ('2026-12-24', '09:00', '11:00', 2, 3)"))
                .isInstanceOf(DataIntegrityViolationException.class) // el tipo de excepción que esperamos que se lance es DataIntegrityViolationException
                .hasMessageContaining("ck_slot_booked_le_capacity"); // el mensaje de la excepción debe contener el nombre de la restricción que se violó
    }

    // V5: cada tabla de ajustes tiene exactamente una fila, con los valores que traía shop_settings
    @Test
    void settingsTablesHaveExactlyOneRowEach() {
        assertThat(jdbc.queryForObject("select count(*) from delivery_settings", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from order_settings", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select max_units_per_order from order_settings", Integer.class)).isEqualTo(20);
        assertThat(jdbc.queryForObject("select default_slot_capacity from delivery_settings", Integer.class)).isEqualTo(4);
    }

    // ADR 0013, punto 2 (regla 5 del ADR 0001): ninguna clave foránea cruza de un módulo a otro.
    // Cada tabla debe estar en este mapa: una tabla nueva sin dueño hace fallar la prueba.
    private static final Map<String, String> TABLE_MODULE = Map.ofEntries(
            Map.entry("category", "catalog"), Map.entry("product", "catalog"),
            Map.entry("product_category", "catalog"), Map.entry("product_variant", "catalog"),
            Map.entry("delivery_zone", "scheduling"), Map.entry("delivery_slot", "scheduling"),
            Map.entry("delivery_settings", "scheduling"),
            Map.entry("customer_order", "ordering"), Map.entry("order_line", "ordering"),
            Map.entry("order_settings", "ordering"),
            Map.entry("payment_method_policy", "payment"));

    @Test
    void everyTableHasAnOwnerModule() {
        List<String> tables = jdbc.queryForList(
                "select table_name from information_schema.tables "
                        + "where table_schema = 'public' and table_type = 'BASE TABLE' "
                        + "and table_name <> 'flyway_schema_history'", String.class);

        assertThat(TABLE_MODULE.keySet()).containsExactlyInAnyOrderElementsOf(tables);
    }

    @Test
    void noForeignKeyCrossesModules() {
        List<Map<String, Object>> foreignKeys = jdbc.queryForList("""
                select tc.table_name as from_table, ccu.table_name as to_table
                from information_schema.table_constraints tc
                join information_schema.constraint_column_usage ccu
                  on ccu.constraint_name = tc.constraint_name and ccu.table_schema = tc.table_schema
                where tc.constraint_type = 'FOREIGN KEY' and tc.table_schema = 'public'
                """);

        assertThat(foreignKeys).isNotEmpty(); // las hay dentro de cada módulo
        for (Map<String, Object> fk : foreignKeys) {
            String from = (String) fk.get("from_table");
            String to = (String) fk.get("to_table");
            assertThat(TABLE_MODULE.get(from))
                    .as("clave foránea %s -> %s", from, to)
                    .isEqualTo(TABLE_MODULE.get(to));
        }
    }

    // V3: un producto nuevo tiene que decir cuánto tarda (la columna no tiene valor por defecto)
    @Test
    void productRequiresCraftingHours() {
        assertThatThrownBy(() -> jdbc.update(
                "insert into product (name, slug) values ('Sin tiempo', 'sin-tiempo-v3')"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    // V3: una variante ocupa al menos un cupo
    @Test
    void variantSlotUnitsMustBePositive() {
        Long productId = jdbc.queryForObject(
                "insert into product (name, slug, crafting_hours) values ('Cupos', 'cupos-v3', 4) returning id",
                Long.class);
        try {
            assertThatThrownBy(() -> jdbc.update(
                    "insert into product_variant (product_id, name, price, slot_units) values (?, 'Cero', 1000, 0)",
                    productId))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("ck_variant_slot_units");
        } finally {
            jdbc.update("delete from product where id = ?", productId);
        }
    }

    // V3 y V4: el margen de despacho arranca en 2 horas
    @Test
    void dispatchMarginStartsAtTwoHours() {
        assertThat(jdbc.queryForObject("select dispatch_margin_hours from delivery_settings", Integer.class))
                .isEqualTo(2);
    }

        // comprueba que la migración V2 creó el índice para buscar productos por categoría
    @Test
    void migrationCreatesProductCategoryIndex() {
        Integer count = jdbc.queryForObject(
                "select count(*) from pg_indexes "
                        + "where tablename = 'product_category' and indexname = 'idx_product_category_category'",
                Integer.class);

        assertThat(count).isEqualTo(1);
    }
}


/* 
Método	                    Para qué	                                Devuelve
queryForList(sql, Tipo)	    Un SELECT con muchas filas de una columna	List<Tipo>
queryForObject(sql, Tipo)	Un SELECT con un solo valor	                Tipo
update(sql)	                INSERT, UPDATE o DELETE	                    int: filas afectadas
*/