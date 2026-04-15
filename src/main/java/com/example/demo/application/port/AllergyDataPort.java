package com.example.demo.application.port;

import com.example.demo.domain.Allergy;

import java.util.List;

public interface AllergyDataPort {

    List<Allergy> findAll();

    Allergy save(Allergy allergy);

    long count();
}
