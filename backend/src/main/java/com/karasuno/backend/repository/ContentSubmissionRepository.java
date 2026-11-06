package com.karasuno.backend.repository;

import com.karasuno.backend.entity.ContentSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentSubmissionRepository extends JpaRepository<ContentSubmission, Long> {

    List<ContentSubmission> findByCreatorUserIdOrderByUpdatedAtDesc(Long creatorUserId);

    Page<ContentSubmission> findByCreatorUserIdOrderByUpdatedAtDesc(Long creatorUserId, Pageable pageable);

    Page<ContentSubmission> findByStatusOrderBySubmittedAtDesc(ContentSubmission.SubmissionStatus status, Pageable pageable);

    Page<ContentSubmission> findAllByOrderByUpdatedAtDesc(Pageable pageable);

    long countByStatus(ContentSubmission.SubmissionStatus status);

    List<ContentSubmission> findTop10ByStatusOrderByUpdatedAtDesc(ContentSubmission.SubmissionStatus status);
}
