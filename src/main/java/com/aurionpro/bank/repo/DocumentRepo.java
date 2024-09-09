package com.aurionpro.bank.repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;

public interface DocumentRepo extends JpaRepository<Document, Long> {

    List<Document> findByCustomer(Customer customer);
}
