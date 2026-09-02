package com.contra.javaassignment.repository;

import com.contra.javaassignment.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionRecord, Long> {
    Optional<TransactionRecord> findByTransactionId(UUID transactionId);
}
