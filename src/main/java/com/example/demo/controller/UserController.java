package com.example.demo.controller;

import com.example.demo.application.service.UserService;
import com.example.demo.model.UserDTO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<UserDTO> getUsers() {
        return userService.getUsers();
    }

    @GetMapping("/name-like/{name}")
    public List<UserDTO> getUsersByNameLike(@PathVariable("name") String name) {
        return userService.findAllByNameLike(name);
    }

    @PostMapping("/{name}")
    public UserDTO createUser(@PathVariable("name") String name) {
        return userService.createUser(name);
    }

    @PutMapping("/update-name/{id}/{name}")
    public void updateUserName(@PathVariable("id") Long id, @PathVariable("name") String name) {
        userService.updateUser(id, name);
    }

    @GetMapping("/allergies")
    public List<UserDTO> getUsersWithAllergies() {
        return userService.getUsersWithAllergies();
    }
}

