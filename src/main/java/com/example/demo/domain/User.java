package com.example.demo.domain;

import com.example.demo.model.UserDTO;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@Setter
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
            name = "user_allergy",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "allergy_id")
    )
    private List<Allergy> allergies;

    public UserDTO toDTO(User user){
        return UserDTO.builder()
                .id(user.getId()).name(user.getName()).allergies(user.getAllergies().stream().map(Allergy::getName).collect(Collectors.toList())).build();

    }
}
