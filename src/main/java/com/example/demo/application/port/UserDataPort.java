package com.example.demo.application.port;

import com.example.demo.domain.User;

import java.util.List;

public interface UserDataPort {

    List<User> findAllWithAllergies();

    List<User> findAllByNameContaining(String name);

    List<User> findAll();

    User save(User user);

    void updateById(Long id, String name);

    long count();
}
