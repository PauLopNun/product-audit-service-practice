package com.example.demo.infrastructure.persistence.rest.dto;

import java.math.BigDecimal;

public record ProductResponse(Long id, String name, String category,
                               BigDecimal price, Integer stock, Boolean active) {}
