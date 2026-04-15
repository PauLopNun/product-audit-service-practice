package com.example.demo.application.service;

import com.example.demo.application.port.ProductAuditPort;
import com.example.demo.application.port.ProductDataPort;
import com.example.demo.domain.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductAuditServiceTest {

    @Mock
    private ProductDataPort productDataPort;

    @Mock
    private ProductAuditPort productAuditPort;

    @InjectMocks
    private ProductAuditService productAuditService;

    private Product product;
    private Product snapshot;

    @BeforeEach
    void setUp() {
        product = new Product();
        product.setId(1L);
        product.setName("iPhone");
        product.setCategory("Electronics");
        product.setPrice(new BigDecimal("999.99"));
        product.setStock(25);
        product.setActive(true);

        snapshot = new Product();
        snapshot.setId(1L);
        snapshot.setName("iPhone 14");
        snapshot.setCategory("Electronics");
        snapshot.setPrice(new BigDecimal("799.99"));
        snapshot.setStock(50);
        snapshot.setActive(true);
    }

    @Test
    void getRevisions_returnsList() {
        List<Number> revisions = List.of(1, 2, 3);
        when(productAuditPort.getRevisions(1L)).thenReturn(revisions);

        List<Number> result = productAuditService.getRevisions(1L);

        assertThat(result).containsExactly(1, 2, 3);
        verify(productAuditPort).getRevisions(1L);
    }

    @Test
    void getProductAtRevision_returnsProduct() {
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(snapshot);

        Product result = productAuditService.getProductAtRevision(1L, 2);

        assertThat(result).isEqualTo(snapshot);
        verify(productAuditPort).getProductAtRevision(1L, 2);
    }

    @Test
    void getProductAtRevision_returnsNullWhenNotFound() {
        when(productAuditPort.getProductAtRevision(1L, 99)).thenReturn(null);

        Product result = productAuditService.getProductAtRevision(1L, 99);

        assertThat(result).isNull();
    }

    @Test
    void revertToRevision_updatesProductAndSaves() {
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(snapshot);
        when(productDataPort.findById(1L)).thenReturn(Optional.of(product));
        when(productDataPort.save(any(Product.class))).thenReturn(product);

        Product result = productAuditService.revertToRevision(1L, 2);

        assertThat(result).isNotNull();
        verify(productDataPort).save(product);
        assertThat(product.getName()).isEqualTo("iPhone 14");
        assertThat(product.getPrice()).isEqualByComparingTo("799.99");
        assertThat(product.getStock()).isEqualTo(50);
    }

    @Test
    void revertToRevision_throwsWhenSnapshotNotFound() {
        when(productAuditPort.getProductAtRevision(1L, 99)).thenReturn(null);

        assertThatThrownBy(() -> productAuditService.revertToRevision(1L, 99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1")
                .hasMessageContaining("99");
    }

    @Test
    void revertToRevision_throwsWhenProductNotFound() {
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(snapshot);
        when(productDataPort.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productAuditService.revertToRevision(1L, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");
    }

    @Test
    void diff_returnsChangedFields() {
        Product fromProduct = new Product();
        fromProduct.setId(1L);
        fromProduct.setName("Old Name");
        fromProduct.setCategory("Electronics");
        fromProduct.setPrice(new BigDecimal("500.00"));
        fromProduct.setStock(10);
        fromProduct.setActive(true);

        Product toProduct = new Product();
        toProduct.setId(1L);
        toProduct.setName("New Name");
        toProduct.setCategory("Electronics");
        toProduct.setPrice(new BigDecimal("600.00"));
        toProduct.setStock(10);
        toProduct.setActive(true);

        when(productAuditPort.getProductAtRevision(1L, 1)).thenReturn(fromProduct);
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(toProduct);

        Map<String, Map<String, Object>> result = productAuditService.diff(1L, 1, 2);

        assertThat(result).containsKeys("name", "price");
        assertThat(result).doesNotContainKeys("category", "stock", "active");
        assertThat(result.get("name").get("from")).isEqualTo("Old Name");
        assertThat(result.get("name").get("to")).isEqualTo("New Name");
    }

    @Test
    void diff_throwsWhenFromRevisionNotFound() {
        when(productAuditPort.getProductAtRevision(1L, 1)).thenReturn(null);
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(snapshot);

        assertThatThrownBy(() -> productAuditService.diff(1L, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");
    }

    @Test
    void diff_throwsWhenToRevisionNotFound() {
        when(productAuditPort.getProductAtRevision(1L, 1)).thenReturn(snapshot);
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(null);

        assertThatThrownBy(() -> productAuditService.diff(1L, 1, 2))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1");
    }

    @Test
    void diff_returnsEmptyMapWhenNoDifferences() {
        Product p1 = new Product();
        p1.setName("Same");
        p1.setCategory("Cat");
        p1.setPrice(new BigDecimal("100.00"));
        p1.setStock(5);
        p1.setActive(true);

        Product p2 = new Product();
        p2.setName("Same");
        p2.setCategory("Cat");
        p2.setPrice(new BigDecimal("100.00"));
        p2.setStock(5);
        p2.setActive(true);

        when(productAuditPort.getProductAtRevision(1L, 1)).thenReturn(p1);
        when(productAuditPort.getProductAtRevision(1L, 2)).thenReturn(p2);

        Map<String, Map<String, Object>> result = productAuditService.diff(1L, 1, 2);

        assertThat(result).isEmpty();
    }

    @Test
    void diff_handlesNullValuesAcrossFields() {
        Product fromProduct = new Product();
        fromProduct.setName(null);
        fromProduct.setCategory("A");
        fromProduct.setPrice(null);
        fromProduct.setStock(3);
        fromProduct.setActive(null);

        Product toProduct = new Product();
        toProduct.setName(null);
        toProduct.setCategory("A");
        toProduct.setPrice(new BigDecimal("50.00"));
        toProduct.setStock(null);
        toProduct.setActive(true);

        when(productAuditPort.getProductAtRevision(1L, 10)).thenReturn(fromProduct);
        when(productAuditPort.getProductAtRevision(1L, 11)).thenReturn(toProduct);

        Map<String, Map<String, Object>> result = productAuditService.diff(1L, 10, 11);

        assertThat(result).doesNotContainKeys("name", "category");
        assertThat(result.get("price").get("from")).isEqualTo("null");
        assertThat(result.get("price").get("to")).isEqualTo(new BigDecimal("50.00"));
        assertThat(result.get("stock").get("from")).isEqualTo(3);
        assertThat(result.get("stock").get("to")).isEqualTo("null");
        assertThat(result.get("active").get("from")).isEqualTo("null");
        assertThat(result.get("active").get("to")).isEqualTo(true);
    }
}
