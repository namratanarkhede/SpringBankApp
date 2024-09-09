package com.aurionpro.bank.service;

import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.CustomerProfileUpdateDTO;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.enums.DocumentType;
import com.aurionpro.bank.exception.CustomerServiceException;

public interface CustomerService {
	CustomerDto validateCustomerLogin(String username, String password);
	//CustomerDto updateCustomerProfile(String username, CustomerDto customerDto);
    void updateCustomerProfile(String username, CustomerProfileUpdateDTO profileUpdateDTO) throws CustomerServiceException;
    void performTransaction(String username, TransactionDto transactionDto);
	PageResponse<TransactionDto> getTransactionsByCustomer(String username, int page, int size);
    String uploadDocument(Integer customerId, MultipartFile file, DocumentType documentType);

    void sendTransactionDetailsByEmail(String username);
}