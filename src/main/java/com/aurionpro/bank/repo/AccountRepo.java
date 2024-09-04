package com.aurionpro.bank.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.aurionpro.bank.entity.Account;
import com.aurionpro.bank.entity.Customer;

public interface AccountRepo extends JpaRepository<Account, String> {
    List<Account> findByCustomer(Customer customer);
    Optional<Account> findByAccountNumber(String accountNumber);

    @Modifying
    @Query("UPDATE Account a SET a.status = 'INACTIVE' WHERE a.customer.id = :customerId")
    void setAccountsStatusToInactiveByCustomerId(int customerId);
}