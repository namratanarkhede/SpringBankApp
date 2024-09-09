package com.aurionpro.bank.service;

import java.io.ByteArrayInputStream;


public interface MailService {
    void sendEmail(String to, String subject, String body);
    void sendAccountCreationEmail(String to, String username, String passwordFormat, String accountNumber, double balance);
    void sendTransactionNotification(String to, String transactionType, double amount, double newBalance, String accountNumber);

    void sendEmailWithAttachment(String to, String subject, String body, String attachmentName, ByteArrayInputStream attachment);

}
