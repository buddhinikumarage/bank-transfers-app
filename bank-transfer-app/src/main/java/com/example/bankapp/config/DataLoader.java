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

        // Sample customers (total = LKR 85,000.00), only on the first run
        if (customerRepository.count() == 0) {
            customerRepository.save(new Customer(null, "Kamal Silva",
                    "kamal@example.com", "100001", 50000.0));
            customerRepository.save(new Customer(null, "Nimali Perera",
                    "nimali@example.com", "100002", 25000.0));
            customerRepository.save(new Customer(null, "Saman Fernando",
                    "saman@example.com", "100003", 10000.0));

            System.out.println(">>> Sample customer data initialized (Total: LKR 85,000.00).");
        }

        // Bank fee account (Task 12). Created if missing, even when data already exists.
        if (customerRepository.getByAccountNumber("999999").isEmpty()) {
            customerRepository.save(new Customer(null, "Bank Fee Account",
                    "fees@bank.local", "999999", 0.0));

            System.out.println(">>> Fee account 999999 created.");
        }
    }
}