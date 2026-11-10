package com.floralshop.backend.catalog.infra.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.floralshop.backend.catalog.application.service.CatalogQueryService;
import com.floralshop.backend.catalog.domain.exception.ProductNotFoundException;
import com.floralshop.backend.catalog.domain.model.Category;
import com.floralshop.backend.catalog.domain.model.PageQuery;
import com.floralshop.backend.catalog.domain.model.PagedResult;
import com.floralshop.backend.catalog.domain.model.Product;
import com.floralshop.backend.catalog.domain.model.ProductVariant;

// Prueba de la capa web: el servicio está simulado, no hay base de datos.
@WebMvcTest(CatalogController.class)
class CatalogControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    CatalogQueryService catalog; // doble del servicio

    private Product product() {
        return new Product(7L, "Ramo primavera", "ramo-primavera", "Flores de temporada", true,
                List.of(new ProductVariant(11L, "Mediano", new BigDecimal("80000.00"), true, 1)),
                List.of(new Category(3L, "Rosas", "rosas", true, 1)));
    }

    @Test
    void listsProductsAsPageWithoutInternalFields() throws Exception {
        when(catalog.listProducts(null, new PageQuery(0, 20)))
                .thenReturn(new PagedResult<>(List.of(product()), 0, 20, 1));

        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Ramo primavera"))
                .andExpect(jsonPath("$.content[0].slug").value("ramo-primavera"))
                .andExpect(jsonPath("$.content[0].variants[0].id").value(11))
                .andExpect(jsonPath("$.content[0].variants[0].price").value(80000.0))
                .andExpect(jsonPath("$.content[0].categories[0].slug").value("rosas"))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.content[0].available").value(true))
                // Lo interno no se expone:
                .andExpect(jsonPath("$.content[0].id").doesNotExist())
                .andExpect(jsonPath("$.content[0].active").doesNotExist())
                .andExpect(jsonPath("$.content[0].categories[0].id").doesNotExist());
    }

    @Test
    void forwardsCategoryAndPagingToTheService() throws Exception {
        when(catalog.listProducts("rosas", new PageQuery(2, 5)))
                .thenReturn(new PagedResult<>(List.of(), 2, 5, 0));

        mockMvc.perform(get("/api/v1/products?category=rosas&page=2&size=5"))
                .andExpect(status().isOk());

        verify(catalog).listProducts("rosas", new PageQuery(2, 5));
    }

    @ParameterizedTest
    @ValueSource(strings = {"size=0", "size=101", "page=-1"})
    void rejectsOutOfRangePagingWith400(String query) throws Exception {
        mockMvc.perform(get("/api/v1/products?" + query))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(catalog); // el servicio ni se entera
    }

    @Test
    void getsProductBySlug() throws Exception {
        when(catalog.getProduct("ramo-primavera")).thenReturn(product());

        mockMvc.perform(get("/api/v1/products/ramo-primavera"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ramo primavera"))
                .andExpect(jsonPath("$.variants[0].name").value("Mediano"));
    }

    @Test
    void missingProductIs404WithProblemFormat() throws Exception {
        when(catalog.getProduct("nada")).thenThrow(new ProductNotFoundException("nada"));

        mockMvc.perform(get("/api/v1/products/nada"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value(containsString("nada")));
    }

    @Test
    void listsCategoriesWithoutInternalFields() throws Exception {
        when(catalog.listCategories()).thenReturn(List.of(new Category(3L, "Rosas", "rosas", true, 1)));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Rosas"))
                .andExpect(jsonPath("$[0].slug").value("rosas"))
                .andExpect(jsonPath("$[0].id").doesNotExist());
    }

    // Las dos siguientes protegen contra el riesgo del manejador global:
    // que su captura de "cualquier error" se trague errores que no son un 500.
    @Test
    void unknownRouteIs404NotA500() throws Exception {
        mockMvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wrongMethodIs405NotA500() throws Exception {
        mockMvc.perform(post("/api/v1/categories"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    void unexpectedErrorIsAGeneric500WithoutLeakingDetails() throws Exception {
        when(catalog.listCategories()).thenThrow(new IllegalStateException("detalle interno secreto"));

        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.detail").value("Error interno del servidor"))
                .andExpect(content().string(not(containsString("secreto"))));
    }
}