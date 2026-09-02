package com.contra.javaassignment.dto;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionRequest(
        UUID transactionId,
        UUID userId,
        BigDecimal amount,
        String type
) {}
