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

    // ------------------------------------------------------------------
    // Dashboard
    // ------------------------------------------------------------------

    @GetMapping("/")
    public String viewDashboard(@RequestParam(required = false) String keyword,
                                Model model) {

        var allCustomers = bankService.getAllCustomersOrdered();
        var transactions = bankService.getAllTransactions();

        var customers = (keyword == null || keyword.isBlank())
                ? allCustomers
                : allCustomers.stream()
                    .filter(c -> c.getName() != null
                            && c.getName().toLowerCase()
                                .contains(keyword.toLowerCase()))
                    .toList();

        double totalBalance = allCustomers.stream()
                .mapToDouble(c -> c.getBalance())
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

        if (!model.containsAttribute("transferRequest")) {
            model.addAttribute("transferRequest", new TransferRequest());
        }
        if (!model.containsAttribute("customer")) {
            model.addAttribute("customer", new Customer());
        }

        return "bank";
    }

    // ------------------------------------------------------------------
    // Transfer
    // ------------------------------------------------------------------

    @PostMapping("/transfer")
    public String handleTransfer(@ModelAttribute TransferRequest transferRequest,
                                 RedirectAttributes redirectAttributes) {
        try {
            bankService.transfer(
                    transferRequest.getSourceAccountNumber(),
                    transferRequest.getTargetAccountNumber(),
                    transferRequest.getAmount(),
                    transferRequest.isSimulateFailure()
            );
            redirectAttributes.addFlashAttribute("success",
                    "Transfer completed successfully!");
        } catch (Exception e) {
            // Runs after the transaction has been rolled back
            redirectAttributes.addFlashAttribute("error",
                    "Transfer failed: " + e.getMessage());
        }
        return "redirect:/";
    }

    // ------------------------------------------------------------------
    // Customers
    // ------------------------------------------------------------------

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

    // ------------------------------------------------------------------
    // Task 12: history, large transfers, deposit, withdraw
    // ------------------------------------------------------------------

    @GetMapping("/accounts/{accountNumber}/history")
    public String history(@PathVariable String accountNumber,
                          Model model,
                          RedirectAttributes redirectAttributes) {
        try {
            Customer holder = bankService.getCustomerByAccountNumber(accountNumber);
            model.addAttribute("holder", holder);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/";
        }
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("transactions", bankService.getAccountHistory(accountNumber));
        model.addAttribute("totalSent", bankService.getTotalSent(accountNumber));
        return "history";
    }

    @GetMapping("/large-transfers")
    public String largeTransfers(@RequestParam(defaultValue = "10000") Double min,
                                 Model model) {
        model.addAttribute("min", min);
        model.addAttribute("transfers", bankService.getLargeTransfers(min));
        return "large-transfers";
    }

    @GetMapping("/deposit")
    public String depositForm(Model model) {
        model.addAttribute("title", "Deposit Money");
        model.addAttribute("action", "/deposit");
        model.addAttribute("allCustomers", bankService.getAllCustomersOrdered());
        return "cash";
    }

    @PostMapping("/deposit")
    public String deposit(@RequestParam String accountNumber,
                          @RequestParam(required = false) Double amount,
                          RedirectAttributes redirectAttributes) {
        try {
            bankService.deposit(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Deposit completed successfully!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Deposit failed: " + e.getMessage());
            return "redirect:/deposit";
        }
    }

    @GetMapping("/withdraw")
    public String withdrawForm(Model model) {
        model.addAttribute("title", "Withdraw Money");
        model.addAttribute("action", "/withdraw");
        model.addAttribute("allCustomers", bankService.getAllCustomersOrdered());
        return "cash";
    }

    @PostMapping("/withdraw")
    public String withdraw(@RequestParam String accountNumber,
                           @RequestParam(required = false) Double amount,
                           RedirectAttributes redirectAttributes) {
        try {
            bankService.withdraw(accountNumber, amount);
            redirectAttributes.addFlashAttribute("success", "Withdrawal completed successfully!");
            return "redirect:/";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Withdrawal failed: " + e.getMessage());
            return "redirect:/withdraw";
        }
    }
}