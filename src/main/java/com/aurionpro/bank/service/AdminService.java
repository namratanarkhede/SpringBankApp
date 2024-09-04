package com.aurionpro.bank.service;

import org.springframework.data.domain.Pageable;

import com.aurionpro.bank.dto.AccountDto;
import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.entity.Bank;
import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;
import com.aurionpro.bank.enums.KycStatus;

public interface AdminService {
    String addCustomer(CustomerDto customerDto);
    String addAccount(AccountDto accountDto);
    String addBank(Bank bank);
    PageResponse<Customer> viewCustomers(Pageable pageable);
    PageResponse<TransactionDto> viewAllTransactions(Pageable pageable);
    String deleteCustomer(int customerId);


    // Method to update KYC status
    void verifyDocument(Long documentId, KycStatus kycStatus);
    Document getDocumentById(Long documentId);

    //void sendEmailToCustomer(int customerId);
    
}