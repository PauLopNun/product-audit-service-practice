package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.application.port.AllergyDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.infrastructure.persistence.rest.dto.AllergyResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
@RequiredArgsConstructor
public class AllergyRestAdapter implements AllergyDataPort {

    private final RestTemplate restTemplate;

    @Value("${app.data-service.url}")
    private String baseUrl;

    @Override
    public List<Allergy> findAll() {
        AllergyResponse[] responses = restTemplate.getForObject(
                baseUrl + "/api/allergies", AllergyResponse[].class);
        return responses == null ? List.of() : Arrays.stream(responses).map(this::toAllergy).toList();
    }

    @Override
    public Allergy save(Allergy allergy) {
        AllergyResponse response = restTemplate.postForObject(
                baseUrl + "/api/allergies",
                Map.of("name", allergy.getName() != null ? allergy.getName() : "",
                       "severity", allergy.getSeverity() != null ? allergy.getSeverity() : ""),
                AllergyResponse.class);
        return toAllergy(response);
    }

    @Override
    public long count() {
        Long count = restTemplate.getForObject(baseUrl + "/api/allergies/count", Long.class);
        return count != null ? count : 0L;
    }

    private Allergy toAllergy(AllergyResponse r) {
        if (r == null) return null;
        Allergy allergy = new Allergy();
        allergy.setId(r.id());
        allergy.setName(r.name());
        allergy.setSeverity(r.severity());
        return allergy;
    }
}
