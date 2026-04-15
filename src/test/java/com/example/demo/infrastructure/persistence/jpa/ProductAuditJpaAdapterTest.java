package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.domain.Product;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAuditJpaAdapterTest {

    @Test
    void getRevisions_usesAuditReaderFactory() {
        EntityManager entityManager = mock(EntityManager.class);
        AuditReader auditReader = mock(AuditReader.class);
        ProductAuditJpaAdapter adapter = new ProductAuditJpaAdapter(entityManager);

        try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
            mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
            when(auditReader.getRevisions(Product.class, 7L)).thenReturn(List.of(1, 2, 3));

            List<Number> revisions = adapter.getRevisions(7L);

            assertThat(revisions).containsExactly(1, 2, 3);
        }
    }

    @Test
    void getProductAtRevision_usesAuditReaderFactory() {
        EntityManager entityManager = mock(EntityManager.class);
        AuditReader auditReader = mock(AuditReader.class);
        ProductAuditJpaAdapter adapter = new ProductAuditJpaAdapter(entityManager);
        Product snapshot = new Product();
        snapshot.setId(3L);

        try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
            mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
            when(auditReader.find(Product.class, 3L, 4)).thenReturn(snapshot);

            Product result = adapter.getProductAtRevision(3L, 4);

            assertThat(result).isEqualTo(snapshot);
        }
    }
}

