package com.aurionpro.bank.service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailServiceImpl implements MailService {

    private final JavaMailSender javaMailSender;

    public MailServiceImpl(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void sendEmail(String to, String subject, String body) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(to);
        message.setSubject(subject);
        message.setText(body);
        javaMailSender.send(message);
    }
    @Override
    public void sendAccountCreationEmail(String to, String fullName, String passwordDescription, String accountNumber, double balance) {
        String subject = "Congratulations! Your Account has been Created";
        String body = String.format(
            "Dear %s,%n%n" +
            "Congratulations! Your account has been successfully created.%n%n" +
            "Your account details are as follows:%n" +
            "Username: %s%n" +
            "Password Format: %s%n%n" +
            "Account Number: %s%n" +
            "Current Balance: $%.2f%n%n" +
            "Thank you for choosing our bank.%n%n" +
            "Best regards,%n" +
            "The Bank Team",
            fullName, to, passwordDescription, accountNumber, balance
        );

        sendEmail(to, subject, body);
    }
    
    @Override
    public void sendTransactionNotification(String to, String transactionType, double amount, double newBalance, String accountNumber) {
        String subject = "Transaction Notification";
        String body;

        switch (transactionType.toUpperCase()) {
            case "TRANSFER":
                body = String.format(
                    "Dear Customer,%n%n" +
                    "A transfer transaction has been processed from your account.%n%n" +
                    "Transaction Details:%n" +
                    "Transaction Type: Transfer%n" +
                    "Amount: $%.2f%n" +
                    "Account Number: %s%n" +
                    "Current Balance: $%.2f%n%n" +
                    "Thank you for banking with us.%n%n" +
                    "Best regards,%n" +
                    "The Bank Team",
                    amount, accountNumber, newBalance
                );
                break;

            case "CREDIT":
                body = String.format(
                    "Dear Customer,%n%n" +
                    "A credit transaction has been processed to your account.%n%n" +
                    "Transaction Details:%n" +
                    "Transaction Type: Credit%n" +
                    "Amount Credited: $%.2f%n" +
                    "Account Number: %s%n" +
                    "Current Balance: $%.2f%n%n" +
                    "Thank you for banking with us.%n%n" +
                    "Best regards,%n" +
                    "The Bank Team",
                    amount, accountNumber, newBalance
                );
                break;

            case "DEBIT":
                body = String.format(
                    "Dear Customer,%n%n" +
                    "A debit transaction has been processed from your account.%n%n" +
                    "Transaction Details:%n" +
                    "Transaction Type: Debit%n" +
                    "Amount Debited: $%.2f%n" +
                    "Account Number: %s%n" +
                    "Current Balance: $%.2f%n%n" +
                    "Thank you for banking with us.%n%n" +
                    "Best regards,%n" +
                    "The Bank Team",
                    amount, accountNumber, newBalance
                );
                break;

            default:
                body = "Dear Customer,%n%n" +
                       "A transaction has been processed on your account.%n%n" +
                       "Transaction Details:%n" +
                       "Transaction Type: %s%n" +
                       "Amount: $%.2f%n" +
                       "Account Number: %s%n" +
                       "Current Balance: $%.2f%n%n" +
                       "Thank you for banking with us.%n%n" +
                       "Best regards,%n" +
                       "The Bank Team";
                body = String.format(body, transactionType, amount, accountNumber, newBalance);
                break;
        }

        sendEmail(to, subject, body);
    }
}
