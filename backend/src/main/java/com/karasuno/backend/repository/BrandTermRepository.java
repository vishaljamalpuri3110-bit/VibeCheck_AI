package com.karasuno.backend.repository;

import com.karasuno.backend.entity.BrandTerm;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandTermRepository extends JpaRepository<BrandTerm, Long> {

    List<BrandTerm> findByActiveTrue();

    List<BrandTerm> findByActiveTrueAndProhibitedTrue();

    List<BrandTerm> findByActiveTrueAndProhibitedFalse();
}
