package com.example.demo.domain;

import com.example.demo.model.AllergyDTO;
import com.example.demo.model.UserDTO;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DomainMappingTest {

    @Test
    void userToDto_mapsIdNameAndAllergyNames() {
        Allergy peanut = new Allergy();
        peanut.setName("Peanut");

        Allergy dust = new Allergy();
        dust.setName("Dust");

        User user = new User();
        user.setId(10L);
        user.setName("Ana");
        user.setAllergies(List.of(peanut, dust));

        UserDTO dto = user.toDTO(user);

        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getName()).isEqualTo("Ana");
        assertThat(dto.getAllergies()).containsExactly("Peanut", "Dust");
    }

    @Test
    void allergyToDto_mapsNameAndSeverity() {
        Allergy allergy = new Allergy();
        allergy.setName("Pollen");
        allergy.setSeverity("LOW");

        AllergyDTO dto = allergy.toDTO(allergy);

        assertThat(dto.getName()).isEqualTo("Pollen");
        assertThat(dto.getSeverity()).isEqualTo("LOW");
    }
}

