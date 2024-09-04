package com.aurionpro.bank.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.enums.DocumentType;
import com.aurionpro.bank.exception.CustomerServiceException;
import com.aurionpro.bank.security.JwtTokenProvider;
import com.aurionpro.bank.service.CustomerService;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateCustomer(@Valid @RequestBody LoginDto loginDto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
        );
        
        SecurityContextHolder.getContext().setAuthentication(authentication);
        
        // Generate JWT token
        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthResponse(jwt));
    }

    @PutMapping("/updateProfile")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<CustomerDto> updateProfile(
            @RequestParam String username,
            @Valid @RequestBody CustomerDto customerDto) {
        try {
            CustomerDto updatedCustomer = customerService.updateCustomerProfile(username, customerDto);
            return ResponseEntity.ok(updatedCustomer);
        } catch (CustomerServiceException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
    }

    @PostMapping("/transactions")
    @PreAuthorize("hasRole('CUSTOMER')")
    @Transactional
    public ResponseEntity<String> performTransaction(@Valid
            @RequestParam String username,
            @RequestBody TransactionDto transactionDto) {
        try {
            customerService.performTransaction(username, transactionDto);
            return ResponseEntity.ok("Transaction completed successfully!");
        } catch (CustomerServiceException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

	@GetMapping("/transactions")
	@PreAuthorize("hasRole('CUSTOMER')")
	public ResponseEntity<PageResponse<TransactionDto>> getTransactionsByCustomer(
	        @Valid @RequestParam String username,
	        @RequestParam(defaultValue = "0") int page,
	        @RequestParam(defaultValue = "10") int size) {
	    try {
	        PageResponse<TransactionDto> transactions = customerService.getTransactionsByCustomer(username, page, size);
	        return ResponseEntity.ok(transactions);
	    } catch (CustomerServiceException e) {
	        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
	    }
	}

    @PreAuthorize("hasRole('CUSTOMER')")
    @PostMapping("/uploadDocument")
    public ResponseEntity<String> uploadDocument(
            @RequestParam Integer customerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam DocumentType documentType) {

        String response = customerService.uploadDocument(customerId, file, documentType);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
