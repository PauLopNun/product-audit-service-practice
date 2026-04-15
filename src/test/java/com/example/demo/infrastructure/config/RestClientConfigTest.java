package com.example.demo.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientConfigTest {

    @Test
    void restTemplate_createsBeanInstance() {
        RestClientConfig config = new RestClientConfig();

        RestTemplate restTemplate = config.restTemplate();

        assertThat(restTemplate).isNotNull();
    }
}

