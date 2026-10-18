package com.rapports.moteur.repository;

import com.rapports.moteur.entity.PersonaProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PersonaProfileRepository extends JpaRepository<PersonaProfile, UUID> {

    @Query("SELECT p FROM PersonaProfile p WHERE p.user.id = :userId")
    Optional<PersonaProfile> findByUser_Id(@Param("userId") UUID userId);

    @Query("SELECT p FROM PersonaProfile p WHERE p.user.email = :email")
    Optional<PersonaProfile> findByUser_Email(@Param("email") String email);

    @Query("SELECT COUNT(p) > 0 FROM PersonaProfile p WHERE p.user.id = :userId")
    boolean existsByUser_Id(@Param("userId") UUID userId);
}

