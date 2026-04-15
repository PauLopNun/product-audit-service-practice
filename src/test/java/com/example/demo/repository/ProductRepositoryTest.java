package com.example.demo.repository;

import com.example.demo.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        Product iphone = new Product();
        iphone.setName("iPhone");
        iphone.setCategory("Electronics");
        iphone.setPrice(new BigDecimal("999.99"));
        iphone.setStock(20);
        iphone.setActive(true);
        productRepository.save(iphone);

        Product mac = new Product();
        mac.setName("MacBook");
        mac.setCategory("Electronics");
        mac.setPrice(new BigDecimal("2599.99"));
        mac.setStock(4);
        mac.setActive(true);
        productRepository.save(mac);

        Product inactive = new Product();
        inactive.setName("Legacy TV");
        inactive.setCategory("Electronics");
        inactive.setPrice(new BigDecimal("499.99"));
        inactive.setStock(8);
        inactive.setActive(false);
        productRepository.save(inactive);

        Product food = new Product();
        food.setName("Protein Bar");
        food.setCategory("Food");
        food.setPrice(new BigDecimal("2.99"));
        food.setStock(300);
        food.setActive(true);
        productRepository.save(food);
    }

    @Test
    void basicDerivedQueries_workAsExpected() {
        assertThat(productRepository.findByName("iPhone")).isPresent();
        assertThat(productRepository.existsByName("MacBook")).isTrue();
        assertThat(productRepository.existsByName("Unknown")).isFalse();
        assertThat(productRepository.countByActiveTrue()).isEqualTo(3);
    }

    @Test
    void findByCategoryAndPriceLessThanAndStockGreaterThanAndActiveTrue_filtersCorrectly() {
        Page<Product> page = productRepository.findByCategoryAndPriceLessThanAndStockGreaterThanAndActiveTrue(
                "Electronics",
                new BigDecimal("1500.00"),
                5,
                PageRequest.of(0, 10)
        );

        assertThat(page.getContent()).extracting(Product::getName).containsExactly("iPhone");
    }

    @Test
    void deleteByCategory_removesProductsForThatCategoryOnly() {
        productRepository.deleteByCategory("Food");

        assertThat(productRepository.findByName("Protein Bar")).isEmpty();
        assertThat(productRepository.findByName("iPhone")).isPresent();
    }
}

