package com.example.demo.controller;

import com.example.demo.application.service.ProductAuditService;
import com.example.demo.domain.Product;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;
    private final ProductAuditService productAuditService;

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<Product> update(@PathVariable Long id,
                                          @RequestBody UpdateProductRequest body) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado: " + id));

        if (body.name()     != null) product.setName(body.name());
        if (body.category() != null) product.setCategory(body.category());
        if (body.price()    != null) product.setPrice(body.price());
        if (body.stock()    != null) product.setStock(body.stock());
        if (body.active()   != null) product.setActive(body.active());

        return ResponseEntity.ok(productRepository.save(product));
    }


    @GetMapping("/{id}/audit/revisions")
    public ResponseEntity<List<Number>> getRevisions(@PathVariable Long id) {
        return ResponseEntity.ok(productAuditService.getRevisions(id));
    }

    @GetMapping("/{id}/audit/{revision}")
    public ResponseEntity<Product> getAtRevision(@PathVariable Long id,
                                                 @PathVariable Integer revision) {
        Product product = productAuditService.getProductAtRevision(id, revision);
        if (product == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(product);
    }

    @PostMapping("/{id}/audit/revert/{revision}")
    public ResponseEntity<Product> revert(@PathVariable Long id,
                                          @PathVariable Integer revision) {
        return ResponseEntity.ok(productAuditService.revertToRevision(id, revision));
    }

    @GetMapping("/{id}/audit/diff")
    public ResponseEntity<Map<String, Map<String, Object>>> diff(
            @PathVariable Long id,
            @RequestParam Integer from,
            @RequestParam Integer to) {
        return ResponseEntity.ok(productAuditService.diff(id, from, to));
    }

    record UpdateProductRequest(String name, String category,
                                BigDecimal price, Integer stock, Boolean active) {}
}
