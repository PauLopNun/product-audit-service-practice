package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.application.port.ProductAuditPort;
import com.example.demo.domain.Product;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class ProductAuditJpaAdapter implements ProductAuditPort {

    private final EntityManager entityManager;

    @Override
    public List<Number> getRevisions(Long productId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.getRevisions(Product.class, productId);
    }

    @Override
    public Product getProductAtRevision(Long productId, Integer revision) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(Product.class, productId, revision);
    }
}
