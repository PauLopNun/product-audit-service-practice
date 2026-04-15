package com.example.demo.infrastructure.seeder;

import com.example.demo.application.port.AllergyDataPort;
import com.example.demo.application.port.UserDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final UserDataPort userDataPort;
    private final AllergyDataPort allergyDataPort;

    @Override
    public void run(String... args) {
        if (userDataPort.count() > 0 || allergyDataPort.count() > 0) {
            return;
        }

        List<Allergy> allergies = new ArrayList<>();

        for (int j = 1; j <= 5; j++) {
            Allergy allergy = new Allergy();
            allergy.setName("Allergy " + j);
            allergyDataPort.save(allergy);
            allergies.add(allergy);
        }

        for (int i = 1; i <= 30; i++) {
            User user = new User();
            user.setName("User " + i);
            user.setAllergies(allergies);
            userDataPort.save(user);
        }
    }
}
