package com.example.demo.infrastructure.persistence.rest.dto;

import java.util.List;

public record UserResponse(Long id, String name, List<AllergyResponse> allergies) {}
