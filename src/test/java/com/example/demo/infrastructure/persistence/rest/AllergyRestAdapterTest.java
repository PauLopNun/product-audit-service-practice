package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.domain.Allergy;
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

class AllergyRestAdapterTest {

    private WireMockServer wireMockServer;
    private AllergyRestAdapter adapter;

    private static final String ALLERGY_JSON =
            "{\"id\":1,\"name\":\"Peanuts\",\"severity\":\"HIGH\"}";
    private static final String ALLERGIES_ARRAY_JSON =
            "[{\"id\":1,\"name\":\"Peanuts\",\"severity\":\"HIGH\"}," +
            "{\"id\":2,\"name\":\"Dust\",\"severity\":\"MEDIUM\"}]";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        RestTemplate restTemplate = new RestTemplate();
        adapter = new AllergyRestAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void findAll_returnsAllAllergies() {
        wireMockServer.stubFor(get(urlEqualTo("/api/allergies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(ALLERGIES_ARRAY_JSON)));

        List<Allergy> result = adapter.findAll();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Peanuts");
        assertThat(result.get(0).getSeverity()).isEqualTo("HIGH");
        assertThat(result.get(1).getName()).isEqualTo("Dust");
    }

    @Test
    void findAll_returnsEmptyList_whenNull() {
        wireMockServer.stubFor(get(urlEqualTo("/api/allergies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));

        List<Allergy> result = adapter.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_returnsEmptyList_whenNoContent() {
        wireMockServer.stubFor(get(urlEqualTo("/api/allergies"))
                .willReturn(aResponse().withStatus(204)));

        List<Allergy> result = adapter.findAll();

        assertThat(result).isEmpty();
    }

    @Test
    void save_postsAndReturnsAllergy() {
        wireMockServer.stubFor(post(urlEqualTo("/api/allergies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(ALLERGY_JSON)));

        Allergy allergy = new Allergy();
        allergy.setName("Peanuts");
        allergy.setSeverity("HIGH");

        Allergy result = adapter.save(allergy);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Peanuts");
        assertThat(result.getSeverity()).isEqualTo("HIGH");
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/allergies")));
    }

    @Test
    void save_withNullFields_sendsFallbackValues() {
        wireMockServer.stubFor(post(urlEqualTo("/api/allergies"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(ALLERGY_JSON)));

        Allergy allergy = new Allergy();

        Allergy result = adapter.save(allergy);

        assertThat(result).isNotNull();
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/allergies"))
                .withRequestBody(matchingJsonPath("$.name", equalTo("")))
                .withRequestBody(matchingJsonPath("$.severity", equalTo(""))));
    }

    @Test
    void save_returnsNull_whenNoContent() {
        wireMockServer.stubFor(post(urlEqualTo("/api/allergies"))
                .willReturn(aResponse().withStatus(204)));

        Allergy result = adapter.save(new Allergy());

        assertThat(result).isNull();
    }

    @Test
    void count_returnsCount() {
        wireMockServer.stubFor(get(urlEqualTo("/api/allergies/count"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("7")));

        long result = adapter.count();

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void count_returnsZeroWhenBodyIsNull() {
        wireMockServer.stubFor(get(urlEqualTo("/api/allergies/count"))
                .willReturn(aResponse().withStatus(204)));

        long result = adapter.count();

        assertThat(result).isZero();
    }
}
