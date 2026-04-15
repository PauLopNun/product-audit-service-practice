package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.application.port.ProductAuditPort;
import com.example.demo.domain.Product;
import com.example.demo.infrastructure.persistence.rest.dto.ProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
@RequiredArgsConstructor
public class ProductAuditRestAdapter implements ProductAuditPort {

    private final RestTemplate restTemplate;

    @Value("${app.data-service.url}")
    private String baseUrl;

    @Override
    public List<Number> getRevisions(Long productId) {
        List<Integer> revisions = restTemplate.exchange(
                baseUrl + "/api/products/{id}/audit/revisions",
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<List<Integer>>() {},
                productId
        ).getBody();
        return revisions == null ? List.of() : List.copyOf(revisions);
    }

    @Override
    public Product getProductAtRevision(Long productId, Integer revision) {
        try {
            ProductResponse response = restTemplate.getForObject(
                    baseUrl + "/api/products/{id}/audit/{revision}",
                    ProductResponse.class,
                    productId, revision);
            return toProduct(response);
        } catch (HttpClientErrorException.NotFound e) {
            return null;
        }
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
