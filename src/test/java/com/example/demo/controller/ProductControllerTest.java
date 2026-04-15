package com.example.demo.controller;

import com.example.demo.application.port.ProductDataPort;
import com.example.demo.application.service.ProductAuditService;
import com.example.demo.domain.Product;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductDataPort productDataPort;

    @MockitoBean
    private ProductAuditService productAuditService;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    private Product buildProduct() {
        Product p = new Product();
        p.setId(1L);
        p.setName("iPhone");
        p.setCategory("Electronics");
        p.setPrice(new BigDecimal("999.99"));
        p.setStock(25);
        p.setActive(true);
        return p;
    }

    @Test
    void update_found_returns200() throws Exception {
        Product existing = buildProduct();
        Product saved = buildProduct();
        saved.setName("iPhone 15");

        when(productDataPort.findById(1L)).thenReturn(Optional.of(existing));
        when(productDataPort.save(any())).thenReturn(saved);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"iPhone 15\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone 15"));
    }

    @Test
    void update_withAllFields_updatesEverything() throws Exception {
        Product existing = buildProduct();
        Product saved = buildProduct();
        saved.setName("Pixel 9");
        saved.setCategory("Mobile");
        saved.setPrice(new BigDecimal("1199.99"));
        saved.setStock(8);
        saved.setActive(false);

        when(productDataPort.findById(1L)).thenReturn(Optional.of(existing));
        when(productDataPort.save(any())).thenReturn(saved);

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Pixel 9\",\"category\":\"Mobile\",\"price\":1199.99,\"stock\":8,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Pixel 9"))
                .andExpect(jsonPath("$.category").value("Mobile"))
                .andExpect(jsonPath("$.price").value(1199.99))
                .andExpect(jsonPath("$.stock").value(8))
                .andExpect(jsonPath("$.active").value(false));
    }

    @Test
    void update_withEmptyBody_keepsOriginalValues() throws Exception {
        Product existing = buildProduct();

        when(productDataPort.findById(1L)).thenReturn(Optional.of(existing));
        when(productDataPort.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        mockMvc.perform(put("/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"))
                .andExpect(jsonPath("$.category").value("Electronics"))
                .andExpect(jsonPath("$.price").value(999.99))
                .andExpect(jsonPath("$.stock").value(25))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void update_notFound_returns400() {
        when(productDataPort.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> mockMvc.perform(put("/products/99")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Test\"}")))
                .isInstanceOf(ServletException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Producto no encontrado");
    }

    @Test
    void getRevisions_returns200WithList() throws Exception {
        when(productAuditService.getRevisions(1L)).thenReturn(List.of(1, 2, 3));

        mockMvc.perform(get("/products/1/audit/revisions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0]").value(1));
    }

    @Test
    void getAtRevision_found_returns200() throws Exception {
        when(productAuditService.getProductAtRevision(1L, 2)).thenReturn(buildProduct());

        mockMvc.perform(get("/products/1/audit/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("iPhone"));
    }

    @Test
    void getAtRevision_notFound_returns404() throws Exception {
        when(productAuditService.getProductAtRevision(1L, 99)).thenReturn(null);

        mockMvc.perform(get("/products/1/audit/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void revert_returns200() throws Exception {
        when(productAuditService.revertToRevision(1L, 2)).thenReturn(buildProduct());

        mockMvc.perform(post("/products/1/audit/revert/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void diff_returns200WithChanges() throws Exception {
        Map<String, Map<String, Object>> changes = Map.of(
                "name", Map.of("from", "Old", "to", "New")
        );
        when(productAuditService.diff(1L, 1, 2)).thenReturn(changes);

        mockMvc.perform(get("/products/1/audit/diff")
                        .param("from", "1")
                        .param("to", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name.from").value("Old"))
                .andExpect(jsonPath("$.name.to").value("New"));
    }
}
