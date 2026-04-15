package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.application.port.ProductDataPort;
import com.example.demo.domain.Product;
import com.example.demo.infrastructure.persistence.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
@RequiredArgsConstructor
public class ProductRestAdapter implements ProductDataPort {

    private final RestTemplate restTemplate;

    @Value("${app.data-service.url}")
    private String baseUrl;

    @Override
    public Optional<Product> findById(Long id) {
        try {
            ProductResponse response = restTemplate.getForObject(
                    baseUrl + "/api/products/{id}", ProductResponse.class, id);
            return Optional.ofNullable(toProduct(response));
        } catch (HttpClientErrorException.NotFound e) {
            return Optional.empty();
        }
    }

    @Override
    public Product save(Product product) {
        ResponseEntity<ProductResponse> response = restTemplate.exchange(
                baseUrl + "/api/products/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(product),
                ProductResponse.class,
                product.getId());
        return toProduct(response.getBody());
    }

    private Product toProduct(ProductResponse r) {
        if (r == null) return null;
        Product product = new Product();
        product.setId(r.id());
        product.setName(r.name());
        product.setCategory(r.category());
        product.setPrice(r.price());
        product.setStock(r.stock());
        product.setActive(r.active());
        return product;
    }
}
