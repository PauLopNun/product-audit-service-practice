package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.domain.Allergy;
import com.example.demo.repository.AllergyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllergyJpaAdapterTest {

    @Mock
    private AllergyRepository allergyRepository;

    @InjectMocks
    private AllergyJpaAdapter adapter;

    @Test
    void findAll_save_and_count_delegateToRepository() {
        Allergy allergy = new Allergy();
        allergy.setId(1L);

        when(allergyRepository.findAll()).thenReturn(List.of(allergy));
        when(allergyRepository.save(allergy)).thenReturn(allergy);
        when(allergyRepository.count()).thenReturn(2L);

        assertThat(adapter.findAll()).containsExactly(allergy);
        assertThat(adapter.save(allergy)).isEqualTo(allergy);
        assertThat(adapter.count()).isEqualTo(2L);
    }
}

