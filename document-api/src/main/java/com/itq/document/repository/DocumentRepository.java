package com.itq.document.repository;

import com.itq.document.entity.Document;
import com.itq.document.enums.DocumentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") Long id);

    @Query("SELECT d FROM Document d WHERE d.id IN :ids")
    List<Document> findAllByIds(@Param("ids") List<Long> ids);

    @Query("SELECT d FROM Document d WHERE d.status = :status ORDER BY d.id ASC LIMIT :limit")
    List<Document> findByStatusWithLimit(@Param("status") DocumentStatus status, @Param("limit") int limit);

    @Query("""
        SELECT d FROM Document d
        WHERE (:status IS NULL OR d.status = :status)
          AND (:author IS NULL OR LOWER(d.author) LIKE LOWER(CONCAT('%', :author, '%')))
          AND (:from IS NULL OR d.createdAt >= :from)
          AND (:to IS NULL OR d.createdAt <= :to)
        """)
    Page<Document> search(
            @Param("status") DocumentStatus status,
            @Param("author") String author,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable
    );
}
