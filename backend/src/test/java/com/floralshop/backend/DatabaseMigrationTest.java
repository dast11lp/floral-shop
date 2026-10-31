package com.floralshop.backend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

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
                "delivery_zone", "delivery_slot", "shop_settings");
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

    // comprueba que actualm ente tengamos justamente una migración
    @Test
    void shopSettingsHasExactlyOneRow() {
        Integer rows = jdbc.queryForObject("select count(*) from shop_settings", Integer.class); // ejecuta una consulta que devuelve exactamente un valor y dámelo, convertido a este tipo

        assertThat(rows).isEqualTo(1);
    }
}


/* 
Método	                    Para qué	                                Devuelve
queryForList(sql, Tipo)	    Un SELECT con muchas filas de una columna	List<Tipo>
queryForObject(sql, Tipo)	Un SELECT con un solo valor	                Tipo
update(sql)	                INSERT, UPDATE o DELETE	                    int: filas afectadas
*/