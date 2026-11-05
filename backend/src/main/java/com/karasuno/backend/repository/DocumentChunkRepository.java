package com.karasuno.backend.repository;

import com.karasuno.backend.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

/** JPA repository for DocumentChunk — supports search and retrieval for compliance matching. */
@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, Long> {

    /** All chunks for a document, ordered by page then chunk index */
    List<DocumentChunk> findByDocumentNameOrderByPageNumberAscChunkIndexAsc(String documentName);

    /** All chunks for a specific page of a document */
    List<DocumentChunk> findByDocumentNameAndPageNumber(String documentName, Integer pageNumber);

    /** Case-insensitive keyword search across all chunk content */
    @Query("SELECT dc FROM DocumentChunk dc WHERE LOWER(dc.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DocumentChunk> searchByKeyword(@Param("keyword") String keyword);

    /** Distinct document names for the Document Manager list view */
    @Query("SELECT DISTINCT dc.documentName FROM DocumentChunk dc ORDER BY dc.documentName")
    List<String> findDistinctDocumentNames();

    /** Total chunk count for a document */
    long countByDocumentName(String documentName);

    /** Delete all chunks for a document (supports re-ingestion) */
    void deleteByDocumentName(String documentName);

    /** Chunks filtered by document type */
    List<DocumentChunk> findByDocumentTypeOrderByDocumentNameAscPageNumberAsc(String documentType);
}
