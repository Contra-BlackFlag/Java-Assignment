package com.contra.javaassignment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "transactions", indexes ={
        @Index(name = "idx_tx_id", columnList = "transactionId", unique = true)
})
public class TransactionRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private UUID transactionId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private String type;

    public TransactionRecord() {}
    public TransactionRecord(UUID transactionId, UUID userId, BigDecimal amount, String type) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.amount = amount;
        this.type = type;
    }

    public UUID getTransactionId() { return transactionId; }
    public BigDecimal getAmount() { return amount; }
}
