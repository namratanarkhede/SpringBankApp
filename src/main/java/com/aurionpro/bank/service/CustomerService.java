package com.aurionpro.bank.service;

import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.enums.DocumentType;

public interface CustomerService {
	CustomerDto validateCustomerLogin(String username, String password);
	CustomerDto updateCustomerProfile(String username, CustomerDto customerDto);
    void performTransaction(String username, TransactionDto transactionDto);
	PageResponse<TransactionDto> getTransactionsByCustomer(String username, int page, int size);
    String uploadDocument(Integer customerId, MultipartFile file, DocumentType documentType);


}