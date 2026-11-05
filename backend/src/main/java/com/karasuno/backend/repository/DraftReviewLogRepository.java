package com.karasuno.backend.repository;

import com.karasuno.backend.entity.DraftReviewLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

/** JPA repository for DraftReviewLog — supports manager dashboard queries. */
@Repository
public interface DraftReviewLogRepository extends JpaRepository<DraftReviewLog, Long> {

    /** All drafts by a specific user, newest first */
    List<DraftReviewLog> findBySubmittedByUserIdOrderBySubmittedAtDesc(Long userId);

    /** Paginated drafts by review status, oldest first (for manager queue) */
    Page<DraftReviewLog> findByReviewStatusOrderBySubmittedAtAsc(
            DraftReviewLog.ReviewStatus status, Pageable pageable);

    /** All drafts paginated, newest first */
    Page<DraftReviewLog> findAllByOrderBySubmittedAtDesc(Pageable pageable);

    /** Count by review status (dashboard statistics) */
    long countByReviewStatus(DraftReviewLog.ReviewStatus status);
}
