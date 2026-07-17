package com.luckyby.personaldocs.document;

import java.util.*;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    Optional<Document> findByIdAndOwnerId(Long id, Long ownerId);
}
