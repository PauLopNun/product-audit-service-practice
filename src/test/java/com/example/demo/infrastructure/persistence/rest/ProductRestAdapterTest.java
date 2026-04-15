package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.domain.Product;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Optional;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProductRestAdapterTest {

    private WireMockServer wireMockServer;
    private ProductRestAdapter adapter;

    private static final String PRODUCT_JSON =
            "{\"id\":1,\"name\":\"iPhone\",\"category\":\"Electronics\",\"price\":999.99,\"stock\":25,\"active\":true}";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        RestTemplate restTemplate = new RestTemplate();
        adapter = new ProductRestAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void findById_found_returnsProduct() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCT_JSON)));

        Optional<Product> result = adapter.findById(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getName()).isEqualTo("iPhone");
        assertThat(result.get().getCategory()).isEqualTo("Electronics");
        assertThat(result.get().getPrice()).isEqualByComparingTo("999.99");
        assertThat(result.get().getStock()).isEqualTo(25);
        assertThat(result.get().getActive()).isTrue();
    }

    @Test
    void findById_notFound_returnsEmpty() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/99"))
                .willReturn(aResponse()
                        .withStatus(404)));

        Optional<Product> result = adapter.findById(99L);

        assertThat(result).isEmpty();
    }

    @Test
    void findById_noContent_returnsEmpty() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/2"))
                .willReturn(aResponse()
                        .withStatus(204)));

        Optional<Product> result = adapter.findById(2L);

        assertThat(result).isEmpty();
    }

    @Test
    void save_sendsAndReturnsProduct() {
        wireMockServer.stubFor(put(urlEqualTo("/api/products/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCT_JSON)));

        Product product = new Product();
        product.setId(1L);
        product.setName("iPhone");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStock(25);
        product.setActive(true);

        Product result = adapter.save(product);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("iPhone");
        wireMockServer.verify(putRequestedFor(urlEqualTo("/api/products/1")));
    }
}
