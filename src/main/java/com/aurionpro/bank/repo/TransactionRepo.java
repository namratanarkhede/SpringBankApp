package com.aurionpro.bank.repo;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Transaction;

public interface TransactionRepo extends JpaRepository<Transaction, Long> {
    Page<Transaction> findBySenderAccount_Customer(Customer customer, Pageable pageable);
    Page<Transaction> findByReceiverAccount_Customer(Customer customer, Pageable pageable);
}
