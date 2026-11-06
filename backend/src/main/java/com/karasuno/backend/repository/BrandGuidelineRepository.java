package com.karasuno.backend.repository;

import com.karasuno.backend.entity.BrandGuideline;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BrandGuidelineRepository extends JpaRepository<BrandGuideline, Long> {

    Optional<BrandGuideline> findFirstByActiveTrueOrderByUpdatedAtDesc();

    List<BrandGuideline> findAllByOrderByCreatedAtDesc();
}
