package com.karasuno.backend.repository;

import com.karasuno.backend.entity.SubmissionComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SubmissionCommentRepository extends JpaRepository<SubmissionComment, Long> {

    List<SubmissionComment> findBySubmissionIdOrderByCreatedAtAsc(Long submissionId);

    long countBySubmissionId(Long submissionId);
}
