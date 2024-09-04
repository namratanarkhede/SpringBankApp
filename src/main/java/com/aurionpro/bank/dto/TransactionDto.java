package com.aurionpro.bank.dto;

import java.time.LocalDateTime;

import com.aurionpro.bank.enums.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDto {

    private Long transactionId;

    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType; 

    @DecimalMin(value = "0.0", inclusive = true, message = "Transaction amount must be positive")
    private double transactionAmount;

    @NotBlank(message = "Sender account number is required")
    private String senderAccountNumber; 

    private String receiverAccountNumber;
}
