package com.example.demo.infrastructure.persistence.rest;

import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import com.example.demo.infrastructure.persistence.rest.dto.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "rest")
@RequiredArgsConstructor
public class UserRestAdapter implements UserDataPort {

    private final RestTemplate restTemplate;

    @Value("${app.data-service.url}")
    private String baseUrl;

    @Override
    public List<User> findAllWithAllergies() {
        UserResponse[] responses = restTemplate.getForObject(baseUrl + "/api/users", UserResponse[].class);
        return responses == null ? List.of() : Arrays.stream(responses).map(this::toUser).toList();
    }

    @Override
    public List<User> findAllByNameContaining(String name) {
        UserResponse[] responses = restTemplate.getForObject(
                baseUrl + "/api/users?nameContains={name}", UserResponse[].class, name);
        return responses == null ? List.of() : Arrays.stream(responses).map(this::toUser).toList();
    }

    @Override
    public List<User> findAll() {
        return findAllWithAllergies();
    }

    @Override
    public User save(User user) {
        if (user.getId() == null) {
            UserResponse response = restTemplate.postForObject(
                    baseUrl + "/api/users",
                    Map.of("name", user.getName()),
                    UserResponse.class);
            return toUser(response);
        }
        ResponseEntity<UserResponse> response = restTemplate.exchange(
                baseUrl + "/api/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", user.getName())),
                UserResponse.class,
                user.getId());
        return toUser(response.getBody());
    }

    @Override
    public void updateById(Long id, String name) {
        restTemplate.exchange(
                baseUrl + "/api/users/{id}",
                HttpMethod.PUT,
                new HttpEntity<>(Map.of("name", name)),
                Void.class,
                id);
    }

    @Override
    public long count() {
        Long count = restTemplate.getForObject(baseUrl + "/api/users/count", Long.class);
        return count != null ? count : 0L;
    }

    private User toUser(UserResponse r) {
        if (r == null) return null;
        User user = new User();
        user.setId(r.id());
        user.setName(r.name());
        List<Allergy> allergies = r.allergies() == null ? List.of() :
                r.allergies().stream().map(ar -> {
                    Allergy allergy = new Allergy();
                    allergy.setId(ar.id());
                    allergy.setName(ar.name());
                    allergy.setSeverity(ar.severity());
                    return allergy;
                }).toList();
        user.setAllergies(allergies);
        return user;
    }
}
