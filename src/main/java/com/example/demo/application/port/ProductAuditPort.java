package com.example.demo.application.port;

import com.example.demo.domain.Product;

import java.util.List;

public interface ProductAuditPort {

    List<Number> getRevisions(Long productId);

    Product getProductAtRevision(Long productId, Integer revision);
}
