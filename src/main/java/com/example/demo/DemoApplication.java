package com.example.demo;

import com.example.demo.domain.Allergy;
import com.example.demo.domain.User;
import com.example.demo.repository.AllergyRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class DemoApplication {

	public static void main(String[] args) {
		SpringApplication.run(DemoApplication.class, args);
	}

	@Bean
	CommandLineRunner initData(UserRepository userRepository,
	                           AllergyRepository allergyRepository) {

		return args -> {

							if (userRepository.count() > 0 || allergyRepository.count() > 0) {
								return;
							}

			List<Allergy> allergies = new ArrayList<>();

			for (int j = 1; j <= 5; j++) {
				Allergy allergy = new Allergy();
				allergy.setName("Allergy " + j);

				allergyRepository.save(allergy);
				allergies.add(allergy);
			}

			for (int i = 1; i <= 30; i++) {

				User user = new User();
				user.setName("User " + i);

				user.setAllergies(allergies);

				userRepository.save(user);
			}
		};
	}
}
