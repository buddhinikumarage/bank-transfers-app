package com.example.bankapp.controller;

import com.example.bankapp.dto.TransferRequest;
import com.example.bankapp.model.Customer;
import com.example.bankapp.service.BankService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class BankController {

    private final BankService bankService;

    @GetMapping("/")
    public String viewDashboard(@RequestParam(required = false) String keyword,
                                Model model) {

        var allCustomers = bankService.getAllCustomersOrdered();
        var transactions = bankService.getAllTransactions();

        // Customers table (optional search by name)
        var customers = (keyword == null || keyword.isBlank())
                ? allCustomers
                : allCustomers.stream()
                    .filter(c -> c.getName() != null
                            && c.getName().toLowerCase()
                                .contains(keyword.toLowerCase()))
                    .toList();

        // Statistics
        double totalBalance = allCustomers.stream()
                .mapToDouble(c -> ((Number) c.getBalance()).doubleValue())
                .sum();

        long successCount = transactions.stream()
                .filter(t -> "SUCCESS".equals(t.getStatus().name()))
                .count();

        long failedCount = transactions.size() - successCount;

        model.addAttribute("allCustomers", allCustomers);
        model.addAttribute("customers", customers);
        model.addAttribute("transactions", transactions);
        model.addAttribute("keyword", keyword);

        model.addAttribute("totalCustomers", allCustomers.size());
        model.addAttribute("totalBalance", totalBalance);
        model.addAttribute("successCount", successCount);
        model.addAttribute("failedCount", failedCount);

        // Form backing objects (flash attributes may already have set these)
        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }

        return "bank";
    }

    @PostMapping("/transfer")
    public String handleTransfer(@ModelAttribute TransferRequest transferRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            bankService.transfer(
                    transferRequest.getSourceAccountNumber(),
                    transferRequest.getTargetAccountNumber(),
                    transferRequest.getAmount()
            );
            redirectAttributes.addFlashAttribute("success",
                    "Transfer completed successfully!");
        } catch (Exception e) {
            // The catch block runs after the transaction has rolled back
            redirectAttributes.addFlashAttribute("error",
                    "Transfer failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    // bank.html posts to /customers
    @PostMapping({"/customers", "/customer/create"})
    public String createCustomer(@ModelAttribute Customer customer,
                                 RedirectAttributes redirectAttributes) {
        try {
            bankService.createCustomer(customer);
            redirectAttributes.addFlashAttribute("success",
                    "Customer created successfully!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error",
                    "Failed to create customer: " + e.getMessage());
        }
        return "redirect:/";
    }
}