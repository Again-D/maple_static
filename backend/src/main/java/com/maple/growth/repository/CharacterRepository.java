package com.maple.growth.repository;

import java.util.Optional;
import java.util.UUID;

import com.maple.growth.entity.CharacterEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CharacterRepository extends JpaRepository<CharacterEntity, UUID> {
    Optional<CharacterEntity> findByCharacterName(String characterName);
}
