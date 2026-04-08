package com.example.demo.domain;

import com.example.demo.model.AllergyDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
public class Allergy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String severity;

    @ManyToMany(mappedBy = "allergies")
    private List<User> users = new ArrayList<>();

    public AllergyDTO toDTO(Allergy allergy){
        return AllergyDTO.builder()
                .name(allergy.name)
                .severity(allergy.severity)
                .build();
    }
}
