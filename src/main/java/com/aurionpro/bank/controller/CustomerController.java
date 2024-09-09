package com.aurionpro.bank.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerProfileUpdateDTO;
import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.enums.DocumentType;
import com.aurionpro.bank.exception.CustomerServiceException;
import com.aurionpro.bank.security.JwtTokenProvider;
import com.aurionpro.bank.service.CustomerService;
import com.google.code.kaptcha.impl.DefaultKaptcha;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {
    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @Autowired
    private CustomerService customerService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String captchaText = defaultKaptcha.createText();
        BufferedImage captchaImage = defaultKaptcha.createImage(captchaText);

        request.getSession().setAttribute("captcha", captchaText);

        response.setContentType("image/jpeg");
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(captchaImage, "jpg", outputStream);
        outputStream.close();
    }

    public boolean verifyCaptcha(HttpServletRequest request, String captcha) {
        String captchaSession = (String) request.getSession().getAttribute("captcha");
        return captcha != null && captcha.equals(captchaSession);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> authenticateCustomer(@Valid @RequestBody LoginDto loginDto, HttpServletRequest request) {

        if (!verifyCaptcha(request, loginDto.getCaptcha())) {
            throw new IllegalArgumentException("Invalid CAPTCHA");
        }

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getUsername(), loginDto.getPassword())
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        String jwt = tokenProvider.generateToken(authentication);
        return ResponseEntity.ok(new JwtAuthResponse(jwt));
    }

    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    @PutMapping("/updateProfile")
    public ResponseEntity<String> updateCustomerProfile(@RequestParam String username, @Validated @RequestBody CustomerProfileUpdateDTO profileUpdateDTO) {
    	try {
    		customerService.updateCustomerProfile(username, profileUpdateDTO);
    		return ResponseEntity.ok("Customer profile updated successfully");
    	} catch (CustomerServiceException e) {
    		return ResponseEntity.badRequest().body(e.getMessage());
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
    
    @PostMapping("/sendTransactionDetails")
    @PreAuthorize("hasRole('ROLE_CUSTOMER')")
    public ResponseEntity<String> sendTransactionDetails(
            @RequestParam String username) {
        try {
            customerService.sendTransactionDetailsByEmail(username);
            return ResponseEntity.ok("Transaction details have been sent to your email.");
        } catch (CustomerServiceException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send transaction details: " + e.getMessage());
        }
    }
}
