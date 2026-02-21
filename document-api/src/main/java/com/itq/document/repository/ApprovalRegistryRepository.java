package com.itq.document.repository;

import com.itq.document.entity.ApprovalRegistry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovalRegistryRepository extends JpaRepository<ApprovalRegistry, Long> {

    boolean existsByDocumentId(Long documentId);
}
