package com.example.demo.domain;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class ProductAndRevisionInfoTest {

    @Test
    void product_gettersAndSetters_workAsExpected() {
        Product product = new Product();

        product.setId(11L);
        product.setName("Keyboard");
        product.setCategory("Peripherals");
        product.setPrice(new BigDecimal("59.99"));
        product.setStock(30);
        product.setActive(true);

        assertThat(product.getId()).isEqualTo(11L);
        assertThat(product.getName()).isEqualTo("Keyboard");
        assertThat(product.getCategory()).isEqualTo("Peripherals");
        assertThat(product.getPrice()).isEqualByComparingTo("59.99");
        assertThat(product.getStock()).isEqualTo(30);
        assertThat(product.getActive()).isTrue();
    }

    @Test
    void revisionInfo_getters_returnPersistedValues() {
        RevisionInfo revisionInfo = new RevisionInfo();

        ReflectionTestUtils.setField(revisionInfo, "rev", 42);
        ReflectionTestUtils.setField(revisionInfo, "revtstmp", 1710000000000L);

        assertThat(revisionInfo.getRev()).isEqualTo(42);
        assertThat(revisionInfo.getRevtstmp()).isEqualTo(1710000000000L);
    }
}

