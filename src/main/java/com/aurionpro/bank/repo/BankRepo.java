package com.aurionpro.bank.repo;

import org.springframework.data.jpa.repository.JpaRepository;

import com.aurionpro.bank.entity.Bank;

public interface BankRepo extends JpaRepository<Bank, Long>{

}
