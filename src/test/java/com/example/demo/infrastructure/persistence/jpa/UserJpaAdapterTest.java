package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.domain.User;
import com.example.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserJpaAdapterTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserJpaAdapter adapter;

    @Test
    void delegatesAllMethodsToRepository() {
        User user = new User();
        user.setId(1L);
        user.setName("User");

        when(userRepository.findAllWithAllergies()).thenReturn(List.of(user));
        when(userRepository.findAllByNameContaining("Us")).thenReturn(List.of(user));
        when(userRepository.findAll()).thenReturn(List.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userRepository.count()).thenReturn(3L);

        assertThat(adapter.findAllWithAllergies()).containsExactly(user);
        assertThat(adapter.findAllByNameContaining("Us")).containsExactly(user);
        assertThat(adapter.findAll()).containsExactly(user);
        assertThat(adapter.save(user)).isEqualTo(user);
        assertThat(adapter.count()).isEqualTo(3L);

        adapter.updateById(5L, "Nuevo");
        verify(userRepository).updateById(5L, "Nuevo");
    }
}

