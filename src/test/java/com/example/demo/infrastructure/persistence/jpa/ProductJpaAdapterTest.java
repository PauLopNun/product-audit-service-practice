package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.domain.Product;
import com.example.demo.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductJpaAdapterTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductJpaAdapter adapter;

    @Test
    void findById_delegatesToRepository() {
        Product product = new Product();
        product.setId(9L);

        when(productRepository.findById(9L)).thenReturn(Optional.of(product));

        assertThat(adapter.findById(9L)).contains(product);
    }

    @Test
    void save_delegatesToRepository() {
        Product product = new Product();
        product.setId(11L);

        when(productRepository.save(product)).thenReturn(product);

        assertThat(adapter.save(product)).isEqualTo(product);
    }
}

