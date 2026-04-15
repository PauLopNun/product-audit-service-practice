package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.domain.Product;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

class ProductAuditRestAdapterTest {

    private WireMockServer wireMockServer;
    private ProductAuditRestAdapter adapter;

    private static final String PRODUCT_JSON =
            "{\"id\":1,\"name\":\"iPhone\",\"category\":\"Electronics\",\"price\":999.99,\"stock\":25,\"active\":true}";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        RestTemplate restTemplate = new RestTemplate();
        adapter = new ProductAuditRestAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void getRevisions_returnsListOfRevisions() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1/audit/revisions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[1, 2, 3]")));

        List<Number> result = adapter.getRevisions(1L);

        assertThat(result).hasSize(3);
    }

    @Test
    void getRevisions_returnsEmptyList_whenNullBody() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1/audit/revisions"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        List<Number> result = adapter.getRevisions(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getRevisions_returnsEmptyList_whenNoContent() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/2/audit/revisions"))
                .willReturn(aResponse()
                        .withStatus(204)));

        List<Number> result = adapter.getRevisions(2L);

        assertThat(result).isEmpty();
    }

    @Test
    void getProductAtRevision_found_returnsProduct() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1/audit/2"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(PRODUCT_JSON)));

        Product result = adapter.getProductAtRevision(1L, 2);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("iPhone");
        assertThat(result.getCategory()).isEqualTo("Electronics");
        assertThat(result.getStock()).isEqualTo(25);
        assertThat(result.getActive()).isTrue();
    }

    @Test
    void getProductAtRevision_notFound_returnsNull() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1/audit/99"))
                .willReturn(aResponse()
                        .withStatus(404)));

        Product result = adapter.getProductAtRevision(1L, 99);

        assertThat(result).isNull();
    }

    @Test
    void getProductAtRevision_noContent_returnsNull() {
        wireMockServer.stubFor(get(urlEqualTo("/api/products/1/audit/5"))
                .willReturn(aResponse()
                        .withStatus(204)));

        Product result = adapter.getProductAtRevision(1L, 5);

        assertThat(result).isNull();
    }
}
