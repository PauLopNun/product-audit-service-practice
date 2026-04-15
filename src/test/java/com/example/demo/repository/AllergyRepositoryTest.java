package com.example.demo.repository;

import com.example.demo.domain.Allergy;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AllergyRepositoryTest {

    @Autowired
    private AllergyRepository allergyRepository;

    @Test
    void saveAndFindAll_persistsEntity() {
        Allergy allergy = new Allergy();
        allergy.setName("Dust");
        allergy.setSeverity("MEDIUM");

        allergyRepository.save(allergy);

        assertThat(allergyRepository.findAll())
                .extracting(Allergy::getName)
                .contains("Dust");
    }
}

