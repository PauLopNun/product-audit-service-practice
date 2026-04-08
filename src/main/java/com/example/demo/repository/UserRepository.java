package com.example.demo.repository;

import com.example.demo.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    List<User> findAllByNameContaining(String name);

    @Query("SELECT u FROM User u JOIN FETCH u.allergies")
    List<User> findAllWithAllergies();

    @Modifying
    @Query("UPDATE User u SET u.name = :name WHERE u.id = :id")
    void updateById(@Param("id") Long id, @Param("name") String name);
}
