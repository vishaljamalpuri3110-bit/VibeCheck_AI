package com.karasuno.backend.repository;

import com.karasuno.backend.entity.BrandFactClaim;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BrandFactClaimRepository extends JpaRepository<BrandFactClaim, Long> {

    List<BrandFactClaim> findByActiveTrue();
}
