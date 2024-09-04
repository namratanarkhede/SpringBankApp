package com.aurionpro.bank.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import com.aurionpro.bank.entity.Document;

public interface DocumentRepo extends JpaRepository<Document, Long> {

}
