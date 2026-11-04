
package com.example.bankapp.service;
import org.springframework.transaction.annotation.Propagation;
import com.example.bankapp.exception.AccountNotFoundException;
import com.example.bankapp.exception.InsufficientBalanceException;
import com.example.bankapp.exception.InvalidTransactionException;
import com.example.bankapp.model.BankTransaction;
import com.example.bankapp.model.Customer;
import com.example.bankapp.model.TransactionStatus;
import com.example.bankapp.repository.BankTransactionRepository;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true) // Default for all methods: read-only transaction (reports, searches)[cite: 4]
@RequiredArgsConstructor // Generates the constructor that injects the two repositories[cite: 4]
public class BankService {

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final TransactionAuditService auditService;

    // Read-write transaction for saving a customer[cite: 4]
    @Transactional
    public Customer createCustomer(Customer customer) {
        long exists = customerRepository.countAccounts(customer.getAccountNumber(), customer.getEmail());
        if (exists > 0) {
            throw new InvalidTransactionException("Account number or email already exists.");
        }
        if (customer.getBalance() == null || customer.getBalance() < 0) {
            customer.setBalance(0.0);
        }
        return customerRepository.save(customer);
    }

    public List<Customer> getAllCustomersOrdered() {
        return customerRepository.findAllOrdered();
    }

    public Customer getCustomerByAccountNumber(String accountNumber) {
        return customerRepository.getByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException("Customer not found with account: " + accountNumber));
    }

    // Overrides the class default with a read-write transaction that rolls back for any exception[cite: 4]
   // @Transactional(rollbackFor = Exception.class)
   @Transactional(propagation = Propagation.SUPPORTS)
    public BankTransaction transfer(String sourceAcc, String targetAcc, Double amount) {
        try {
            // 1. Validation checks
            if (sourceAcc.equals(targetAcc)) {
                throw new InvalidTransactionException("Cannot transfer money to the same account.");
            }
            if (amount == null || amount <= 0) {
                throw new InvalidTransactionException("Transfer amount must be greater than zero.");
            }

            Customer sender = customerRepository.getByAccountNumber(sourceAcc)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAcc));

           // Check if target account exists first
customerRepository.getByAccountNumber(targetAcc)
        .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + targetAcc));

            if (sender.getBalance() < amount) {
                throw new InsufficientBalanceException("Insufficient balance in source account.");
            }

            // 2. Perform atomic debit & credit updates
            int updatedRowsDebit = customerRepository.debit(sourceAcc, amount);
            if (updatedRowsDebit == 0) {
                throw new InsufficientBalanceException("Concurrent modification or insufficient balance during debit.");
            }

            int updatedRowsCredit = customerRepository.credit(targetAcc, amount);
            if (updatedRowsCredit == 0) {
                // If credit fails, throw exception to trigger rollback of debit
                throw new RuntimeException("Failed to credit target account.");
            }

            // 3. Save successful transaction audit log
            BankTransaction successTx = new BankTransaction();
            successTx.setSourceAccountNumber(sourceAcc);
            successTx.setTargetAccountNumber(targetAcc);
            successTx.setAmount(amount);
            successTx.setTimestamp(LocalDateTime.now());
            successTx.setStatus(TransactionStatus.SUCCESS);
            successTx.setDescription("Transfer completed successfully.");

            return transactionRepository.save(successTx);

        } catch (Exception e) {
            // Record failure using REQUIRES_NEW propagation so it commits despite outer rollback[cite: 4]
            auditService.recordFailedTransaction(sourceAcc, targetAcc, amount, e.getMessage());
            // Re-throw to ensure the main transfer transaction rolls back changes properly
            throw e;
        }
    }

    public List<BankTransaction> getAllTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }
}