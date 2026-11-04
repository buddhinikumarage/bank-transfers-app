package com.example.bankapp.repository;

import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    // 1. All transactions, newest first
    @Query("SELECT t FROM BankTransaction t ORDER BY t.timestamp DESC")
    List<BankTransaction> findAllLatest();

    // 2. Transactions where the account is either the sender OR the receiver
    @Query("SELECT t FROM BankTransaction t WHERE t.sourceAccountNumber = :accountNumber OR t.targetAccountNumber = :accountNumber ORDER BY t.timestamp DESC")
    List<BankTransaction> findHistory(@Param("accountNumber") String accountNumber);

    // 3. Filters by status (passing an enum parameter) newest first
    @Query("SELECT t FROM BankTransaction t WHERE t.status = :status ORDER BY t.timestamp DESC")
    List<BankTransaction> findByStatusLatest(@Param("status") TransactionStatus status);

    // 4. Number of SUCCESS or FAILED transactions for the dashboard
    @Query("SELECT COUNT(t) FROM BankTransaction t WHERE t.status = :status")
    long countByStatus(@Param("status") TransactionStatus status);
}