package com.example.demo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.boot.SpringApplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;

class DemoApplicationMainTest {

    @Test
    void constructor_canBeInstantiated() {
        assertThat(new DemoApplication()).isNotNull();
    }

    @Test
    void main_delegatesToSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = mockStatic(SpringApplication.class)) {
            DemoApplication.main(new String[]{"--test"});
            springApp.verify(() -> SpringApplication.run(DemoApplication.class, new String[]{"--test"}));
        }
    }
}

