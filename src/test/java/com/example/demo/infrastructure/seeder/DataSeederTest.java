package com.example.demo.infrastructure.seeder;

import com.example.demo.application.port.AllergyDataPort;
import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DataSeederTest {

    @Mock
    private UserDataPort userDataPort;

    @Mock
    private AllergyDataPort allergyDataPort;

    @InjectMocks
    private DataSeeder dataSeeder;

    @Test
    void run_doesNothingWhenDataAlreadyExists() {
        when(userDataPort.count()).thenReturn(1L);

        dataSeeder.run();

        verify(allergyDataPort, never()).save(any(Allergy.class));
        verify(userDataPort, never()).save(any(User.class));
    }

    @Test
    void run_doesNothingWhenOnlyAllergiesExist() {
        when(userDataPort.count()).thenReturn(0L);
        when(allergyDataPort.count()).thenReturn(1L);

        dataSeeder.run();

        verify(allergyDataPort, never()).save(any(Allergy.class));
        verify(userDataPort, never()).save(any(User.class));
    }

    @Test
    void run_seedsAllergiesAndUsersWhenDatabaseIsEmpty() {
        when(userDataPort.count()).thenReturn(0L);
        when(allergyDataPort.count()).thenReturn(0L);

        dataSeeder.run();

        ArgumentCaptor<Allergy> allergyCaptor = ArgumentCaptor.forClass(Allergy.class);
        verify(allergyDataPort, times(5)).save(allergyCaptor.capture());
        assertThat(allergyCaptor.getAllValues())
                .extracting(Allergy::getName)
                .containsExactly("Allergy 1", "Allergy 2", "Allergy 3", "Allergy 4", "Allergy 5");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userDataPort, times(30)).save(userCaptor.capture());

        List<User> savedUsers = userCaptor.getAllValues();
        assertThat(savedUsers)
                .extracting(User::getName)
                .contains("User 1", "User 15", "User 30");
        assertThat(savedUsers).allSatisfy(u -> assertThat(u.getAllergies()).hasSize(5));
    }
}

