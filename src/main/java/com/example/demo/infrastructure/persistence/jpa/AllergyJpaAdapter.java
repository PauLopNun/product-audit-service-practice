package com.example.demo.infrastructure.persistence.jpa;

import com.example.demo.application.port.AllergyDataPort;
import com.example.demo.domain.Allergy;
import com.example.demo.repository.AllergyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "app.datasource.mode", havingValue = "jpa", matchIfMissing = true)
@RequiredArgsConstructor
public class AllergyJpaAdapter implements AllergyDataPort {

    private final AllergyRepository allergyRepository;

    @Override
    public List<Allergy> findAll() {
        return allergyRepository.findAll();
    }

    @Override
    public Allergy save(Allergy allergy) {
        return allergyRepository.save(allergy);
    }

    @Override
    public long count() {
        return allergyRepository.count();
    }
}
