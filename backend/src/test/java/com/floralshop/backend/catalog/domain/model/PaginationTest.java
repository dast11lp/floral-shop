package com.floralshop.backend.catalog.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

// Java puro: sin Spring ni base de datos.
class PaginationTest {

    // ---- PageQuery ----

    @Test
    void pageQueryAcceptsValidValues() {
        PageQuery query = new PageQuery(2, 20);

        assertThat(query.page()).isEqualTo(2);
        assertThat(query.size()).isEqualTo(20);
    }

    @Test
    void pageQueryAcceptsMaxSize() {
        assertThat(new PageQuery(0, PageQuery.MAX_SIZE).size()).isEqualTo(PageQuery.MAX_SIZE);
    }

    @Test
    void pageQueryRejectsNegativePage() {
        assertThatThrownBy(() -> new PageQuery(-1, 10))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageQueryRejectsZeroSize() {
        assertThatThrownBy(() -> new PageQuery(0, 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pageQueryRejectsSizeOverMax() {
        assertThatThrownBy(() -> new PageQuery(0, PageQuery.MAX_SIZE + 1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- PagedResult ----

    @Test
    void totalPagesRoundsUp() {
        // 25 elementos de a 10: dos páginas llenas y una con 5
        assertThat(new PagedResult<>(List.of(), 0, 10, 25).totalPages()).isEqualTo(3);
    }

    @Test
    void totalPagesIsExactWhenDivisible() {
        assertThat(new PagedResult<>(List.of(), 0, 10, 20).totalPages()).isEqualTo(2);
    }

    @Test
    void totalPagesIsZeroWhenThereAreNoElements() {
        assertThat(new PagedResult<>(List.of(), 0, 10, 0).totalPages()).isZero();
    }

    @Test
    void pagedResultRejectsNegativeTotal() {
        assertThatThrownBy(() -> new PagedResult<>(List.of(), 0, 10, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void pagedResultContentIsACopy() {
        List<String> original = new ArrayList<>(List.of("a"));
        PagedResult<String> result = new PagedResult<>(original, 0, 10, 1);

        original.add("b"); // si el resultado no copiara la lista, vería este cambio

        assertThat(result.content()).containsExactly("a");
    }
}