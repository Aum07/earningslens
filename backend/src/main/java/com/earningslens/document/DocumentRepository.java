package com.earningslens.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * SPRING DATA CONCEPT: we only write the interface. Spring generates the class at startup,
 * including the SQL for existsByContentSha256 from the method name.
 */
public interface DocumentRepository extends JpaRepository<DocumentEntity, UUID> {

    boolean existsByContentSha256(String contentSha256);
}
