package com.example.demo.repository;

import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AllergyRepository allergyRepository;

    @Autowired
    private EntityManager entityManager;

    private Allergy peanut;

    @BeforeEach
    void setUp() {
        peanut = new Allergy();
        peanut.setName("Peanut");
        peanut.setSeverity("HIGH");
        peanut = allergyRepository.save(peanut);

        User alice = new User();
        alice.setName("Alice");
        alice.setAllergies(List.of(peanut));
        userRepository.save(alice);

        User bob = new User();
        bob.setName("Bob");
        bob.setAllergies(List.of());
        userRepository.save(bob);
    }

    @Test
    void findAllByNameContaining_returnsMatchingUsers() {
        List<User> result = userRepository.findAllByNameContaining("Ali");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
    }

    @Test
    void findAllWithAllergies_fetchesAllergyCollection() {
        List<User> result = userRepository.findAllWithAllergies();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Alice");
        assertThat(result.get(0).getAllergies()).extracting(Allergy::getName).containsExactly("Peanut");
    }

    @Test
    void updateById_updatesName() {
        User user = userRepository.findAllByNameContaining("Alice").getFirst();

        userRepository.updateById(user.getId(), "Alice Updated");
        entityManager.flush();
        entityManager.clear();

        User updated = userRepository.findById(user.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Alice Updated");
    }
}

