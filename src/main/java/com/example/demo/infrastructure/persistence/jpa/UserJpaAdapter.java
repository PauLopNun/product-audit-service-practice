package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class UserJpaAdapter implements UserDataPort {

    private final UserRepository userRepository;

    @Override
    public List<User> findAllWithAllergies() {
        return userRepository.findAllWithAllergies();
    }

    @Override
    public List<User> findAllByNameContaining(String name) {
        return userRepository.findAllByNameContaining(name);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    public User save(User user) {
        return userRepository.save(user);
    }

    @Override
    public void updateById(Long id, String name) {
        userRepository.updateById(id, name);
    }

    @Override
    public long count() {
        return userRepository.count();
    }
}
