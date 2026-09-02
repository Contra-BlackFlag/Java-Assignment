package com.contra.javaassignment.controller;

import com.contra.javaassignment.dto.TransactionRequest;
import com.contra.javaassignment.dto.TransactionResponse;
import com.contra.javaassignment.exception.DuplicateTransactionException;
import com.contra.javaassignment.exception.InsufficientFundsException;
import com.contra.javaassignment.service.LedgerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/transactions")
public class TransactionController {

    private final LedgerService ledgerService;

    public TransactionController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    @PostMapping("/process")
    public ResponseEntity<TransactionResponse> process(@RequestBody TransactionRequest request) {
        return ResponseEntity.ok(ledgerService.processTransaction(request));
    }

    @ExceptionHandler(DuplicateTransactionException.class)
    public ResponseEntity<String> handleDuplicate(DuplicateTransactionException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<String> handleInsufficientFunds(InsufficientFundsException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }
}