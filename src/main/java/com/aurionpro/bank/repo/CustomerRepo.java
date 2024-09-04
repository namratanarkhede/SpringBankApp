package com.aurionpro.bank.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aurionpro.bank.entity.Customer;

public interface CustomerRepo extends JpaRepository<Customer, Integer> {
    Optional<Customer> findByEmail(String email);

}
