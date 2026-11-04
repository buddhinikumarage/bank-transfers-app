package com.example.bankapp.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "bank_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BankTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sourceAccountNumber;
    
    private String targetAccountNumber;
    
    private Double amount;
    
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private TransactionStatus status;

    private String description; // Useful for recording failure reasons (e.g., "Insufficient funds", "Account not found")
}