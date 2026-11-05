package com.example.bankapp.service;

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
@Transactional(readOnly = true) // Default for all methods: read-only transaction (reports, searches)
@RequiredArgsConstructor
public class BankService {

    private static final String FEE_ACCOUNT = "999999";
    private static final double FEE_RATE = 0.01; // 1%

    private final CustomerRepository customerRepository;
    private final BankTransactionRepository transactionRepository;
    private final TransactionAuditService auditService;

    // ------------------------------------------------------------------
    // Customers
    // ------------------------------------------------------------------

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
                .orElseThrow(() -> new AccountNotFoundException(
                        "Customer not found with account: " + accountNumber));
    }

    // ------------------------------------------------------------------
    // Transfer: withdraw (amount + 1% fee), credit receiver, credit fee account.
    // All of it is ONE transaction. Any exception rolls everything back.
    // ------------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    // @Transactional(propagation = Propagation.SUPPORTS)   // <- Task 10 experiment
    public BankTransaction transfer(String sourceAcc, String targetAcc,
                                    Double amount, boolean simulateFailure) {
        try {
            // 1. Validation
            if (sourceAcc == null || sourceAcc.isBlank()
                    || targetAcc == null || targetAcc.isBlank()) {
                throw new InvalidTransactionException("Please select both sender and receiver.");
            }
            if (sourceAcc.equals(targetAcc)) {
                throw new InvalidTransactionException("Sender and receiver must be different accounts.");
            }
            if (amount == null || amount <= 0) {
                throw new InvalidTransactionException("Transfer amount must be greater than zero.");
            }

            Customer sender = customerRepository.getByAccountNumber(sourceAcc)
                    .orElseThrow(() -> new AccountNotFoundException("Source account not found: " + sourceAcc));

            customerRepository.getByAccountNumber(targetAcc)
                    .orElseThrow(() -> new AccountNotFoundException("Target account not found: " + targetAcc));

            double fee = Math.round(amount * FEE_RATE * 100.0) / 100.0;
            double totalDebit = amount + fee;

            if (sender.getBalance() < totalDebit) {
                throw new InsufficientBalanceException(
                        "Insufficient balance in account " + sourceAcc + " (amount + 1% fee required).");
            }

            // 2. Withdraw from sender (amount + fee)
            if (customerRepository.debit(sourceAcc, totalDebit) == 0) {
                throw new InsufficientBalanceException("Insufficient balance during debit.");
            }

            // 3. Experiment: simulated crash AFTER the withdrawal
            if (simulateFailure) {
                throw new RuntimeException(
                        "Simulated system failure after the withdrawal. All changes were rolled back.");
            }

            // 4. Deposit to receiver
            if (customerRepository.credit(targetAcc, amount) == 0) {
                throw new RuntimeException("Failed to credit target account.");
            }

            // 5. Fee to the bank account 999999 (same transaction)
            if (customerRepository.credit(FEE_ACCOUNT, fee) == 0) {
                throw new RuntimeException("Fee account " + FEE_ACCOUNT + " not found.");
            }

            // 6. Save SUCCESS record
            return transactionRepository.save(buildRecord(sourceAcc, targetAcc, amount,
                    TransactionStatus.SUCCESS,
                    "Transfer completed. Fee LKR " + String.format("%.2f", fee)
                            + " credited to " + FEE_ACCOUNT + "."));

        } catch (Exception e) {
            // Saved in a NEW transaction (REQUIRES_NEW) so it survives the rollback
            auditService.recordFailedTransaction(sourceAcc, targetAcc, amount, e.getMessage());
            throw e; // rethrow so the main transaction rolls back
        }
    }

    // ------------------------------------------------------------------
    // Deposit / Withdraw (Task 12)
    // ------------------------------------------------------------------

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction deposit(String accountNumber, Double amount) {
        if (amount == null || amount <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (customerRepository.credit(accountNumber, amount) == 0) {
            throw new AccountNotFoundException("Account not found: " + accountNumber);
        }
        return transactionRepository.save(buildRecord(null, accountNumber, amount,
                TransactionStatus.SUCCESS, "Deposit"));
    }

    @Transactional(rollbackFor = Exception.class)
    public BankTransaction withdraw(String accountNumber, Double amount) {
        if (amount == null || amount <= 0) {
            throw new InvalidTransactionException("Amount must be greater than zero.");
        }
        if (customerRepository.debit(accountNumber, amount) == 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance or account not found: " + accountNumber);
        }
        return transactionRepository.save(buildRecord(accountNumber, null, amount,
                TransactionStatus.SUCCESS, "Withdrawal"));
    }

    // ------------------------------------------------------------------
    // Reports (read-only)
    // ------------------------------------------------------------------

    public List<BankTransaction> getAllTransactions() {
        return transactionRepository.findAllLatest();
    }

    public List<BankTransaction> getAccountHistory(String accountNumber) {
        return transactionRepository.findHistory(accountNumber);
    }

    public List<BankTransaction> getLargeTransfers(Double min) {
        return transactionRepository.findLargeTransfers(min);
    }

    public Double getTotalSent(String accountNumber) {
        return transactionRepository.totalSentBy(accountNumber, TransactionStatus.SUCCESS);
    }

    // ------------------------------------------------------------------

    private BankTransaction buildRecord(String source, String target, Double amount,
                                        TransactionStatus status, String description) {
        BankTransaction tx = new BankTransaction();
        tx.setSourceAccountNumber(source);
        tx.setTargetAccountNumber(target);
        tx.setAmount(amount);
        tx.setTimestamp(LocalDateTime.now());
        tx.setStatus(status);
        tx.setDescription(description);
        return tx;
    }
}