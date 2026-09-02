package com.contra.javaassignment.dto;


import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID transactionId,
        UUID userId,
        BigDecimal currentBalance,
        String status
) {}
