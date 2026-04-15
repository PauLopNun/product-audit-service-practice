package com.example.demo.application.service;

import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import com.example.demo.model.UserDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserDataPort userDataPort;

    public List<UserDTO> getUsers() {
        List<User> users = userDataPort.findAllWithAllergies();

        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getAllergies().stream()
                                .map(Allergy::getName)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public List<UserDTO> findAllByNameLike(String name) {
        List<User> users = userDataPort.findAllByNameContaining(name);

        return users.stream()
                .map(user -> new UserDTO(
                        user.getId(),
                        user.getName(),
                        user.getAllergies().stream()
                                .map(Allergy::getName)
                                .collect(Collectors.toList())
                ))
                .collect(Collectors.toList());
    }

    public UserDTO createUser(String name) {
        User user = new User();
        user.setName(name);
        user.setAllergies(Collections.emptyList());

        User saved = userDataPort.save(user);
        return mapToDto(saved);
    }

    @Transactional
    public void updateUser(Long id, String name) {
        userDataPort.updateById(id, name);
    }

    public List<UserDTO> getUsersWithAllergies() {
        List<User> users = userDataPort.findAll();

        return users.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    private UserDTO mapToDto(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .name(user.getName())
                .allergies(user.getAllergies()
                        .stream()
                        .map(Allergy::getName)
                        .collect(Collectors.toList()))
                .build();
    }
}
