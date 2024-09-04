package com.aurionpro.bank.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.aurionpro.bank.dto.AccountDto;
import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.entity.Bank;
import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;
import com.aurionpro.bank.enums.KycStatus;
import com.aurionpro.bank.service.AdminService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/customer")
    @Transactional
    public ResponseEntity<String> addCustomer(@Valid @RequestBody CustomerDto customerDto) {
        String response = adminService.addCustomer(customerDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/account")
    @Transactional
    public ResponseEntity<String> addAccount(@Valid @RequestBody AccountDto accountDto) {
        String response = adminService.addAccount(accountDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/customers")
    public ResponseEntity<PageResponse<Customer>> viewCustomers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, size);
        PageResponse<Customer> customers = adminService.viewCustomers(pageable);
        return new ResponseEntity<>(customers, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/transactions")
    public ResponseEntity<PageResponse<TransactionDto>> getAllTransactions(Pageable pageable) {
        PageResponse<TransactionDto> response = adminService.viewAllTransactions(pageable);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/bank")
    public ResponseEntity<String> addBank(@Valid @RequestBody Bank bank) {
        String response = adminService.addBank(bank);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
    
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @DeleteMapping("/customer/{customerId}")
    public ResponseEntity<String> deleteCustomer(@PathVariable int customerId) {
        String response = adminService.deleteCustomer(customerId);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @PostMapping("/verifyDocument")
    public ResponseEntity<String> verifyDocument(
            @RequestParam Long documentId,
            @RequestParam KycStatus kycStatus) {

        adminService.verifyDocument(documentId, kycStatus);
        return new ResponseEntity<>("Document verified successfully!", HttpStatus.OK);
    }

    @PreAuthorize("hasRole('ROLE_ADMIN')")
    @GetMapping("/document/{documentId}")
    public ResponseEntity<Document> getDocumentById(@PathVariable Long documentId) {
        Document document = adminService.getDocumentById(documentId);
        return new ResponseEntity<>(document, HttpStatus.OK);
    }
	
}
