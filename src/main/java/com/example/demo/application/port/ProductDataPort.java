package com.example.demo.application.port;

import com.example.demo.domain.Product;

import java.util.Optional;

public interface ProductDataPort {

    Optional<Product> findById(Long id);

    Product save(Product product);
}
