package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.domain.User;
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

class UserRestAdapterTest {

    private WireMockServer wireMockServer;
    private UserRestAdapter adapter;

    private static final String USER_JSON =
            "{\"id\":1,\"name\":\"User1\",\"allergies\":[{\"id\":1,\"name\":\"Allergy1\",\"severity\":\"HIGH\"}]}";
    private static final String USERS_ARRAY_JSON =
            "[{\"id\":1,\"name\":\"User1\",\"allergies\":[{\"id\":1,\"name\":\"Allergy1\",\"severity\":\"HIGH\"}]}," +
            "{\"id\":2,\"name\":\"User2\",\"allergies\":[]}]";

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        RestTemplate restTemplate = new RestTemplate();
        adapter = new UserRestAdapter(restTemplate);
        ReflectionTestUtils.setField(adapter, "baseUrl", "http://localhost:" + wireMockServer.port());
    }

    @AfterEach
    void tearDown() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void findAllWithAllergies_returnsUsers() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_ARRAY_JSON)));

        List<User> result = adapter.findAllWithAllergies();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("User1");
        assertThat(result.get(0).getAllergies()).hasSize(1);
        assertThat(result.get(0).getAllergies().get(0).getName()).isEqualTo("Allergy1");
        assertThat(result.get(1).getName()).isEqualTo("User2");
    }

    @Test
    void findAllWithAllergies_nullResponse_returnsEmptyList() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users"))
                .willReturn(aResponse().withStatus(204)));

        List<User> result = adapter.findAllWithAllergies();

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByNameContaining_returnsFilteredUsers() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users?nameContains=User1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[" + USER_JSON + "]")));

        List<User> result = adapter.findAllByNameContaining("User1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("User1");
    }

    @Test
    void findAllByNameContaining_mapsNullAllergiesAsEmpty() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users?nameContains=User3"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("[{\"id\":3,\"name\":\"User3\",\"allergies\":null}]")));

        List<User> result = adapter.findAllByNameContaining("User3");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAllergies()).isEmpty();
    }

    @Test
    void findAllByNameContaining_nullResponse_returnsEmptyList() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users?nameContains=NoBody"))
                .willReturn(aResponse().withStatus(204)));

        List<User> result = adapter.findAllByNameContaining("NoBody");

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_delegatesToFindAllWithAllergies() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(USERS_ARRAY_JSON)));

        List<User> result = adapter.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void save_withoutId_postsAndReturnsUser() {
        wireMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        User newUser = new User();
        newUser.setName("User1");
        newUser.setAllergies(List.of());

        User result = adapter.save(newUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("User1");
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/users")));
    }

    @Test
    void save_withoutId_returnsNull_whenServiceReturnsNoBody() {
        wireMockServer.stubFor(post(urlEqualTo("/api/users"))
                .willReturn(aResponse().withStatus(204)));

        User newUser = new User();
        newUser.setName("NoBody");

        User result = adapter.save(newUser);

        assertThat(result).isNull();
    }

    @Test
    void save_withId_putsAndReturnsUser() {
        wireMockServer.stubFor(put(urlEqualTo("/api/users/1"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(USER_JSON)));

        User existingUser = new User();
        existingUser.setId(1L);
        existingUser.setName("User1");
        existingUser.setAllergies(List.of());

        User result = adapter.save(existingUser);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        wireMockServer.verify(putRequestedFor(urlEqualTo("/api/users/1")));
    }

    @Test
    void updateById_sendsAndReturns() {
        wireMockServer.stubFor(put(urlEqualTo("/api/users/2"))
                .willReturn(aResponse()
                        .withStatus(200)));

        adapter.updateById(2L, "Updated");

        wireMockServer.verify(putRequestedFor(urlEqualTo("/api/users/2")));
    }

    @Test
    void count_returnsCount() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users/count"))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("42")));

        long result = adapter.count();

        assertThat(result).isEqualTo(42L);
    }

    @Test
    void count_returnsZeroWhenBodyIsNull() {
        wireMockServer.stubFor(get(urlEqualTo("/api/users/count"))
                .willReturn(aResponse().withStatus(204)));

        long result = adapter.count();

        assertThat(result).isZero();
    }
}
