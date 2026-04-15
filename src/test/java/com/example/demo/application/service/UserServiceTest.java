package com.example.demo.application.service;

import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import com.example.demo.model.UserDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserDataPort userDataPort;

    @InjectMocks
    private UserService userService;

    private User user1;
    private User user2;

    @BeforeEach
    void setUp() {
        Allergy allergy = new Allergy();
        allergy.setId(1L);
        allergy.setName("Peanuts");
        allergy.setSeverity("HIGH");

        user1 = new User();
        user1.setId(1L);
        user1.setName("Alice");
        user1.setAllergies(List.of(allergy));

        user2 = new User();
        user2.setId(2L);
        user2.setName("Bob");
        user2.setAllergies(List.of());
    }

    @Test
    void getUsers_returnsMappedDTOs() {
        when(userDataPort.findAllWithAllergies()).thenReturn(List.of(user1, user2));

        List<UserDTO> result = userService.getUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
        assertThat(result.get(0).getAllergies()).containsExactly("Peanuts");
        assertThat(result.get(1).getName()).isEqualTo("Bob");
        assertThat(result.get(1).getAllergies()).isEmpty();
        verify(userDataPort).findAllWithAllergies();
    }

    @Test
    void getUsers_emptyList_returnsEmpty() {
        when(userDataPort.findAllWithAllergies()).thenReturn(List.of());

        List<UserDTO> result = userService.getUsers();

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByNameLike_returnsMappedDTOs() {
        when(userDataPort.findAllByNameContaining("Ali")).thenReturn(List.of(user1));

        List<UserDTO> result = userService.findAllByNameLike("Ali");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
        assertThat(result.get(0).getAllergies()).containsExactly("Peanuts");
        verify(userDataPort).findAllByNameContaining("Ali");
    }

    @Test
    void createUser_savesAndReturnsDTO() {
        User savedUser = new User();
        savedUser.setId(10L);
        savedUser.setName("Charlie");
        savedUser.setAllergies(List.of());

        when(userDataPort.save(any(User.class))).thenReturn(savedUser);

        UserDTO result = userService.createUser("Charlie");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getName()).isEqualTo("Charlie");
        assertThat(result.getAllergies()).isEmpty();
        verify(userDataPort).save(any(User.class));
    }

    @Test
    void updateUser_delegatesToPort() {
        userService.updateUser(5L, "NewName");

        verify(userDataPort).updateById(5L, "NewName");
    }

    @Test
    void getUsersWithAllergies_returnsMappedDTOs() {
        when(userDataPort.findAll()).thenReturn(List.of(user1, user2));

        List<UserDTO> result = userService.getUsersWithAllergies();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getAllergies()).containsExactly("Peanuts");
        verify(userDataPort).findAll();
    }
}
