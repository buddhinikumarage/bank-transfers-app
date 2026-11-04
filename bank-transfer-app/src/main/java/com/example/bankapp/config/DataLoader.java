package com.example.bankapp.config;

import com.example.bankapp.model.Customer;
import com.example.bankapp.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataLoader implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    public void run(String... args) throws Exception {
        if (customerRepository.count() == 0) {
            Customer c1 = new Customer(null, "Alice Perera", "alice@example.com", "ACC-001", 40000.0);
            Customer c2 = new Customer(null, "Buddhini Kumarage", "buddhini@example.com", "ACC-002", 30000.0);
            Customer c3 = new Customer(null, "Kasun Silva", "kasun@example.com", "ACC-003", 15000.0);

            customerRepository.save(c1);
            customerRepository.save(c2);
            customerRepository.save(c3);

            System.out.println(">>> Sample customer data initialized successfully (Total: LKR 85,000.00).");
        }
    }
}