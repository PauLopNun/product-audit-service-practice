package com.example.demo.application.service;

import com.example.demo.application.port.ProductAuditPort;
import com.example.demo.application.port.ProductDataPort;
import com.example.demo.domain.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductAuditService {

    private final ProductDataPort productDataPort;
    private final ProductAuditPort productAuditPort;

    @Transactional(readOnly = true)
    public List<Number> getRevisions(Long productId) {
        return productAuditPort.getRevisions(productId);
    }

    @Transactional(readOnly = true)
    public Product getProductAtRevision(Long productId, Integer revision) {
        return productAuditPort.getProductAtRevision(productId, revision);
    }

    @Transactional
    public Product revertToRevision(Long productId, Integer revision) {
        Product snapshot = productAuditPort.getProductAtRevision(productId, revision);
        if (snapshot == null) {
            throw new IllegalArgumentException(
                    "No existe el producto %d en la revisión %d".formatted(productId, revision));
        }
        Product current = productDataPort.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + productId));

        current.setName(snapshot.getName());
        current.setCategory(snapshot.getCategory());
        current.setPrice(snapshot.getPrice());
        current.setStock(snapshot.getStock());
        current.setActive(snapshot.getActive());

        return productDataPort.save(current);
    }

    @Transactional(readOnly = true)
    public Map<String, Map<String, Object>> diff(Long productId, Integer revFrom, Integer revTo) {
        Product from = productAuditPort.getProductAtRevision(productId, revFrom);
        Product to   = productAuditPort.getProductAtRevision(productId, revTo);

        if (from == null || to == null) {
            throw new IllegalArgumentException(
                    "Revisión no encontrada para el producto " + productId);
        }

        Map<String, Map<String, Object>> changes = new LinkedHashMap<>();
        addIfDifferent(changes, "name",     from.getName(),     to.getName());
        addIfDifferent(changes, "category", from.getCategory(), to.getCategory());
        addIfDifferent(changes, "price",    from.getPrice(),    to.getPrice());
        addIfDifferent(changes, "stock",    from.getStock(),    to.getStock());
        addIfDifferent(changes, "active",   from.getActive(),   to.getActive());
        return changes;
    }

    private void addIfDifferent(Map<String, Map<String, Object>> result,
                                String field, Object from, Object to) {
        if (from == null && to == null) return;
        if (from != null && from.equals(to)) return;
        result.put(field, Map.of("from", from != null ? from : "null",
                                 "to",   to   != null ? to   : "null"));
    }
}
