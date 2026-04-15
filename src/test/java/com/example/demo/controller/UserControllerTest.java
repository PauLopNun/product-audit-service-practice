package com.example.demo.controller;

import com.example.demo.application.service.UserService;
import com.example.demo.model.UserDTO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @Test
    void getUsers_returnsListOfUsers() throws Exception {
        UserDTO user1 = new UserDTO(1L, "Alice", List.of("Peanuts"));
        UserDTO user2 = new UserDTO(2L, "Bob", List.of());
        when(userService.getUsers()).thenReturn(List.of(user1, user2));

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"))
                .andExpect(jsonPath("$[0].allergies[0]").value("Peanuts"))
                .andExpect(jsonPath("$[1].name").value("Bob"));
    }

    @Test
    void getUsers_returnsEmptyList_whenNoneExist() throws Exception {
        when(userService.getUsers()).thenReturn(List.of());

        mockMvc.perform(get("/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getUsersByNameLike_returnsMatchingUsers() throws Exception {
        UserDTO user = new UserDTO(1L, "Alice", List.of());
        when(userService.findAllByNameLike("Ali")).thenReturn(List.of(user));

        mockMvc.perform(get("/users/name-like/Ali"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Alice"));

        verify(userService).findAllByNameLike("Ali");
    }

    @Test
    void createUser_returnsCreatedUser() throws Exception {
        UserDTO created = new UserDTO(5L, "Charlie", List.of());
        when(userService.createUser("Charlie")).thenReturn(created);

        mockMvc.perform(post("/users/Charlie"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.name").value("Charlie"));

        verify(userService).createUser("Charlie");
    }

    @Test
    void updateUserName_returns200() throws Exception {
        doNothing().when(userService).updateUser(anyLong(), anyString());

        mockMvc.perform(put("/users/update-name/3/NewName"))
                .andExpect(status().isOk());

        verify(userService).updateUser(3L, "NewName");
    }

    @Test
    void getUsersWithAllergies_returnsUserList() throws Exception {
        UserDTO user = new UserDTO(1L, "Alice", List.of("Peanuts", "Dust"));
        when(userService.getUsersWithAllergies()).thenReturn(List.of(user));

        mockMvc.perform(get("/users/allergies"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].allergies.length()").value(2));
    }
}
