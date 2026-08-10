package com.maple.growth.repository;

import java.util.List;
import java.util.UUID;

import com.maple.growth.entity.CollectionRunEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRunRepository extends JpaRepository<CollectionRunEntity, UUID> {
    List<CollectionRunEntity> findByOrderByStartedAtDesc(Pageable pageable);
}
