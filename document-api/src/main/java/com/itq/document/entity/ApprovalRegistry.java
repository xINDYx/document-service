package com.itq.document.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "approval_registry")
public class ApprovalRegistry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "approval_registry_seq")
    @SequenceGenerator(name = "approval_registry_seq", sequenceName = "approval_registry_seq", allocationSize = 50)
    private Long id;

    @Column(name = "document_id", nullable = false, unique = true)
    private Long documentId;

    @Column(name = "document_number", nullable = false, length = 50)
    private String documentNumber;

    @Column(name = "approved_by", nullable = false, length = 255)
    private String approvedBy;

    @Column(name = "approved_at", nullable = false)
    private Instant approvedAt;

    @PrePersist
    protected void onCreate() {
        if (approvedAt == null) {
            approvedAt = Instant.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }

    public String getDocumentNumber() { return documentNumber; }
    public void setDocumentNumber(String documentNumber) { this.documentNumber = documentNumber; }

    public String getApprovedBy() { return approvedBy; }
    public void setApprovedBy(String approvedBy) { this.approvedBy = approvedBy; }

    public Instant getApprovedAt() { return approvedAt; }
    public void setApprovedAt(Instant approvedAt) { this.approvedAt = approvedAt; }
}
