package com.example.demo.repository;

import com.example.demo.domain.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findById(Long id);

    Optional<Product> findByName(String name);

    boolean existsByName(String name);

    long countByActiveTrue();

    @Transactional
    @Modifying
    @Query("DELETE FROM Product p WHERE p.category = :category")
    void deleteByCategory(@Param("category") String category);

    Page<Product> findByCategoryAndPriceLessThanAndStockGreaterThanAndActiveTrue(
            String category,
            BigDecimal price,
            Integer stock,
            Pageable pageable
    );
}
