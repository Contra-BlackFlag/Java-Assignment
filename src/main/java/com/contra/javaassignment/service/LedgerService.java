package com.contra.javaassignment.service;


import com.contra.javaassignment.dto.TransactionRequest;
import com.contra.javaassignment.dto.TransactionResponse;
import com.contra.javaassignment.exception.DuplicateTransactionException;
import com.contra.javaassignment.exception.InsufficientFundsException;
import com.contra.javaassignment.model.TransactionRecord;
import com.contra.javaassignment.model.Wallet;
import com.contra.javaassignment.repository.TransactionRepository;
import com.contra.javaassignment.repository.WalletRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    public LedgerService(WalletRepository walletRepo, TransactionRepository txRepo) {
        this.walletRepository = walletRepo;
        this.transactionRepository = txRepo;
    }

    @Transactional
    public TransactionResponse processTransaction(TransactionRequest request) {
        if (transactionRepository.findByTransactionId(request.transactionId()).isPresent()) {
            throw new DuplicateTransactionException("Duplicate transaction: " + request.transactionId());
        }

        Wallet wallet = walletRepository.findByIdForUpdate(request.userId())
                .orElseThrow(() -> new IllegalArgumentException("Wallet not found"));

        if ("DEBIT".equalsIgnoreCase(request.type())) {
            if (wallet.getBalance().compareTo(request.amount()) < 0) {
                throw new InsufficientFundsException("Insufficient balance.");
            }
            wallet.setBalance(wallet.getBalance().subtract(request.amount()));
        } else if ("CREDIT".equalsIgnoreCase(request.type())) {
            wallet.setBalance(wallet.getBalance().add(request.amount()));
        }

        try {
            transactionRepository.save(new TransactionRecord(
                    request.transactionId(),
                    request.userId(),
                    request.amount(),
                    request.type()
            ));
            walletRepository.save(wallet);
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateTransactionException("Duplicate transaction detected during persist.");
        }

        return new TransactionResponse(request.transactionId(), wallet.getUserId(), wallet.getBalance(), "SUCCESS");
    }
}