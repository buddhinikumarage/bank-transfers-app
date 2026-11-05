package com.example.bankapp.repository;

import com.example.bankapp.model.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    // 1. JPQL + @Param: Finds one customer by account number
    @Query("SELECT c FROM Customer c WHERE c.accountNumber = :accountNumber")
    Optional<Customer> getByAccountNumber(@Param("accountNumber") String accountNumber);

    // 2. JPQL COUNT: Checks whether an account or email already exists
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.accountNumber = :accountNumber OR c.email = :email")
    long countAccounts(@Param("accountNumber") String accountNumber, @Param("email") String email);

    // 3. JPQL ORDER BY: All customers sorted by name
    @Query("SELECT c FROM Customer c ORDER BY c.name ASC")
    List<Customer> findAllOrdered();

    // 4. JPQL LIKE: Case-insensitive name search
    @Query("SELECT c FROM Customer c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Customer> searchByName(@Param("name") String name);

    // 5. JPQL aggregate: Sum of all balances (returns null if no customers)
    @Query("SELECT SUM(c.balance) FROM Customer c")
    Double getTotalBalance();

    // 6. Native SQL: Plain MySQL count
    @Query(value = "SELECT COUNT(*) FROM customers", nativeQuery = true)
    long countCustomersNative();

    // 7. UPDATE + @Modifying: subtracts money only if balance >= amount; returns rows changed (1 or 0)
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance - :amount WHERE c.accountNumber = :accountNumber AND c.balance >= :amount")
    int debit(@Param("accountNumber") String accountNumber, @Param("amount") Double amount);

    // 8. UPDATE + @Modifying: Adds money to an account
    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Customer c SET c.balance = c.balance + :amount WHERE c.accountNumber = :accountNumber")
    int credit(@Param("accountNumber") String accountNumber, @Param("amount") Double amount);
}