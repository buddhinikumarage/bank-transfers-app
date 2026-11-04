package com.example.bankapp.service;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TransactionAuditService {

    private final BankTransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedTransaction(String sourceAcc, String targetAcc, Double amount, String reason) {
        BankTransaction tx = new BankTransaction();
        tx.setSourceAccountNumber(sourceAcc);
        tx.setTargetAccountNumber(targetAcc);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(TransactionStatus.FAILED);
        tx.setDescription(reason);
        
        transactionRepository.save(tx);
    }
}