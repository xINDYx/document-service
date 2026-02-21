package com.itq.document.dto;

import java.util.List;

public record BatchOperationResult(
        List<ItemResult> results
) {
    public record ItemResult(
            Long id,
            String status,
            String message
    ) {
        public static ItemResult success(Long id) {
            return new ItemResult(id, "SUCCESS", null);
        }

        public static ItemResult conflict(Long id, String message) {
            return new ItemResult(id, "CONFLICT", message);
        }

        public static ItemResult notFound(Long id) {
            return new ItemResult(id, "NOT_FOUND", "Document not found");
        }

        public static ItemResult registryError(Long id) {
            return new ItemResult(id, "REGISTRY_ERROR", "Failed to register approval");
        }
    }
}
