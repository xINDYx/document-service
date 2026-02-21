package com.itq.document.repository;

import com.itq.document.entity.DocumentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DocumentHistoryRepository extends JpaRepository<DocumentHistory, Long> {

    @Query("SELECT h FROM DocumentHistory h WHERE h.document.id IN :ids ORDER BY h.performedAt ASC")
    List<DocumentHistory> findAllByDocumentIds(@Param("ids") List<Long> ids);
}
