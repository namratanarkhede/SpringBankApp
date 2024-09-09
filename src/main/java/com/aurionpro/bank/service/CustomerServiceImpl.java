package com.aurionpro.bank.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.CustomerProfileUpdateDTO;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.entity.Account;
import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;
import com.aurionpro.bank.entity.Transaction;
import com.aurionpro.bank.entity.User;
import com.aurionpro.bank.enums.AccountStatus;
import com.aurionpro.bank.enums.DocumentType;
import com.aurionpro.bank.enums.KycStatus;
import com.aurionpro.bank.enums.TransactionType;
import com.aurionpro.bank.exception.CustomerServiceException;
import com.aurionpro.bank.exception.DocumentUploadException;
import com.aurionpro.bank.repo.AccountRepo;
import com.aurionpro.bank.repo.CustomerRepo;
import com.aurionpro.bank.repo.DocumentRepo;
import com.aurionpro.bank.repo.TransactionRepo;
import com.aurionpro.bank.repo.UserRepo;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerRepo customerRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private AccountRepo accountRepo;

    @Autowired
    private TransactionRepo transactionRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;
    
    @Autowired
    private MailService mailService;
    
    @Autowired
    private DocumentRepo documentRepo;
    
    @Autowired
    private Cloudinary cloudinary;

    @Override
    public CustomerDto validateCustomerLogin(String username, String password) {
        logger.info("Validating customer login for username: {}", username);
        
        Customer customer = customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));
        
        if (!passwordEncoder.matches(password, customer.getPassword())) {
            logger.warn("Invalid password for username: {}", username);
            throw new CustomerServiceException("Invalid password");
        }
        
        return new CustomerDto(
            customer.getCustomerId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getDateOfBirth()
        );
    }

    @Transactional
    @Override
    public void updateCustomerProfile(String username, CustomerProfileUpdateDTO profileUpdateDTO) throws CustomerServiceException {
        // Fetch existing customer by email (username)
        Customer existingCustomer = customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));

        // Verify the current password
        if (!passwordEncoder.matches(profileUpdateDTO.getCurrentPassword(), existingCustomer.getPassword())) {
            throw new CustomerServiceException("Current password is incorrect");
        }

        // Update customer details
        existingCustomer.setFirstName(profileUpdateDTO.getFirstName());
        existingCustomer.setLastName(profileUpdateDTO.getLastName());
        existingCustomer.setDateOfBirth(profileUpdateDTO.getDateOfBirth());
        
        // Encrypt and set the new password
        existingCustomer.setPassword(passwordEncoder.encode(profileUpdateDTO.getNewPassword()));

        // Save the updated customer entity
        customerRepo.save(existingCustomer);

        // Fetch corresponding user entity
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new CustomerServiceException("User not found"));

        // Encrypt and update the user password
        user.setPassword(passwordEncoder.encode(profileUpdateDTO.getNewPassword()));

        // Save the updated user entity
        userRepo.save(user);
    }
    
    @Transactional
    @Override
    public void performTransaction(String username, TransactionDto transactionDto) {
        logger.info("Performing transaction for customer: {}, TransactionType: {}", username, transactionDto.getTransactionType());

        Customer customer = findCustomerByUsername(username);
        Account senderAccount = findAccountByNumber(transactionDto.getSenderAccountNumber());

        validateAccountOwnership(senderAccount, customer);

        if (senderAccount.getStatus() != AccountStatus.ACTIVE) {
            logger.error("Transaction failed for customer: {}. Account {} is not active.", username, senderAccount.getAccountNumber());
            throw new CustomerServiceException("Transaction cannot be performed on an inactive account.");
        }

        double transactionAmount = transactionDto.getTransactionAmount();
        TransactionType transactionType = transactionDto.getTransactionType();

        switch (transactionType) {
            case TRANSFER:
                logger.info("Processing transfer transaction for customer: {}, Amount: {}", username, transactionAmount);
                handleTransfer(senderAccount, transactionDto.getReceiverAccountNumber(), transactionAmount, transactionDto);
                break;
            case CREDIT:
                logger.info("Processing credit transaction for customer: {}, Amount: {}", username, transactionAmount);
                handleCredit(senderAccount, transactionAmount, transactionDto);
                break;
            case DEBIT:
                logger.info("Processing debit transaction for customer: {}, Amount: {}", username, transactionAmount);
                handleDebit(senderAccount, transactionAmount, transactionDto);
                break;
            default:
                logger.error("Invalid transaction type for customer: {}", username);
                throw new CustomerServiceException("Invalid transaction type");
        }

        String email = customer.getEmail();
        double newBalance = senderAccount.getBalance();
        mailService.sendTransactionNotification(email, transactionType.toString(), transactionAmount, newBalance, senderAccount.getAccountNumber());

        logger.info("Transaction completed successfully for customer: {}", username);
    }

    private Customer findCustomerByUsername(String username) {
        logger.debug("Finding customer by username: {}", username);
        return customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));
    }

    private Account findAccountByNumber(String accountNumber) {
        logger.debug("Finding account by account number: {}", accountNumber);
        return accountRepo.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new CustomerServiceException("Account not found"));
    }

    private void validateAccountOwnership(Account account, Customer customer) {
        logger.debug("Validating account ownership for customer: {}, Account: {}", customer.getEmail(), account.getAccountNumber());
        if (!account.getCustomer().equals(customer)) {
            logger.warn("Account ownership validation failed for customer: {}, Account: {}", customer.getEmail(), account.getAccountNumber());
            throw new CustomerServiceException("Account does not belong to the customer");
        }
    }

    private void handleTransfer(Account senderAccount, String receiverAccountNumber, double transactionAmount, TransactionDto transactionDto) {
        logger.debug("Handling transfer for sender account: {}, receiver account: {}, amount: {}", 
                     senderAccount.getAccountNumber(), receiverAccountNumber, transactionAmount);
        Account receiverAccount = findAccountByNumber(receiverAccountNumber);

        if (senderAccount.equals(receiverAccount)) {
            logger.warn("Transfer attempted to the same account: {}", senderAccount.getAccountNumber());
            throw new CustomerServiceException("Cannot transfer to the same account");
        }

        if (senderAccount.getBalance() < transactionAmount) {
            logger.warn("Insufficient balance for transfer. Account: {}, Available balance: {}", 
                        senderAccount.getAccountNumber(), senderAccount.getBalance());
            throw new CustomerServiceException("Insufficient balance");
        }

        updateAccountBalances(senderAccount, receiverAccount, transactionAmount);
        saveTransaction(senderAccount, receiverAccount, transactionAmount, transactionDto);
    }

    private void handleCredit(Account account, double transactionAmount, TransactionDto transactionDto) {
        logger.debug("Handling credit for account: {}, amount: {}", account.getAccountNumber(), transactionAmount);

        if (transactionDto.getReceiverAccountNumber() != null) {
            logger.warn("Receiver account number should not be provided for credit transactions. Provided receiver account: {}", transactionDto.getReceiverAccountNumber());
            throw new CustomerServiceException("Receiver account should not be provided for credit transactions");
        }

        account.setBalance(account.getBalance() + transactionAmount);
        accountRepo.save(account);

        saveTransaction(account, null, transactionAmount, transactionDto);
    }

    private void handleDebit(Account account, double transactionAmount, TransactionDto transactionDto) {
        logger.debug("Handling debit for account: {}, amount: {}", account.getAccountNumber(), transactionAmount);

        if (transactionDto.getReceiverAccountNumber() != null) {
            logger.warn("Receiver account number should not be provided for debit transactions. Provided receiver account: {}", transactionDto.getReceiverAccountNumber());
            throw new CustomerServiceException("Receiver account should not be provided for debit transactions");
        }

        if (account.getBalance() < transactionAmount) {
            logger.warn("Insufficient balance for debit. Account: {}, Available balance: {}", account.getAccountNumber(), account.getBalance());
            throw new CustomerServiceException("Insufficient balance");
        }

        account.setBalance(account.getBalance() - transactionAmount);
        accountRepo.save(account);

        saveTransaction(account, null, transactionAmount, transactionDto);
    }

    private void updateAccountBalances(Account senderAccount, Account receiverAccount, double transactionAmount) {
        senderAccount.setBalance(senderAccount.getBalance() - transactionAmount);
        receiverAccount.setBalance(receiverAccount.getBalance() + transactionAmount);

        accountRepo.save(senderAccount);
        accountRepo.save(receiverAccount);
    }

    private void saveTransaction(Account senderAccount, Account receiverAccount, double transactionAmount, TransactionDto transactionDto) {
        Transaction transaction = new Transaction();
        transaction.setSenderAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transaction.setTransactionAmount(transactionAmount);
        transaction.setTransactionType(transactionDto.getTransactionType());

        transactionRepo.save(transaction);
    }

    @Override
    public String uploadDocument(Integer customerId, MultipartFile file, DocumentType documentType) {
        logger.info("Uploading document for customer: {}, DocumentType: {}", customerId, documentType);

        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new DocumentUploadException("Customer not found"));

        if (file.isEmpty()) {
            throw new DocumentUploadException("File is empty");
        }

        try {
            Map<String, String> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap("resource_type", "auto"));
            Document document = new Document();
            document.setDocumentType(documentType);
            document.setDocumentUrl(uploadResult.get("url"));
            document.setCustomer(customer);
            document.setKycStatus(KycStatus.PENDING);;

            documentRepo.save(document);

            logger.info("Document uploaded successfully for customer: {}", customerId);
            return uploadResult.get("url");
        } catch (IOException e) {
            logger.error("Error uploading document for customer: {}", customerId, e);
            throw new DocumentUploadException("Error uploading document", e);
        }
    }

    @Override
    public PageResponse<TransactionDto> getTransactionsByCustomer(String username, int page, int size) {
        logger.info("Retrieving transactions for customer: {} with page: {} and size: {}", username, page, size);

        Customer customer = customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<Transaction> transactionsPage = transactionRepo.findBySenderAccount_Customer(customer, pageable);

        logger.info("Transactions retrieved successfully for customer: {}", username);

        List<TransactionDto> transactionDtos = transactionsPage.getContent().stream()
                .map(transaction -> new TransactionDto(
                        transaction.getTransactionId(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionType(),
                        transaction.getTransactionAmount(),
                        transaction.getSenderAccount().getAccountNumber(),
                        transaction.getReceiverAccount() != null ? transaction.getReceiverAccount().getAccountNumber() : null))
                .collect(Collectors.toList());

        return new PageResponse<>(
                transactionDtos,
                transactionsPage.getNumber(),
                transactionsPage.getSize(),
                transactionsPage.getTotalElements(),
                transactionsPage.getTotalPages(),
                transactionsPage.isLast());
    }
    
    
    @Override
    public void sendTransactionDetailsByEmail(String username){
        // Fetch customer by email (assuming username is the email)
        Customer customer = customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));

        // Fetch transactions with pagination
        Pageable pageable = PageRequest.of(0, 100); // Adjust page size as needed
        Page<Transaction> transactionsPage = transactionRepo.findBySenderAccount_Customer(customer, pageable);

        if (transactionsPage.isEmpty()) {
            throw new CustomerServiceException("No transactions found for the customer");
        }

        // Convert transactions to CSV
        ByteArrayInputStream csvStream = createCsv(transactionsPage.getContent());

        // Send email with attachment
        mailService.sendEmailWithAttachment(customer.getEmail(), "Transaction Details",
                "Please find attached the details of your transactions.", "transactions.csv", csvStream);
    }

    private ByteArrayInputStream createCsv(List<Transaction> transactions) throws CustomerServiceException {
        final String CSV_HEADER = "Transaction ID,Transaction Date,Transaction Type,Transaction Amount,Sender Account,Receiver Account\n";

        // Create StringWriter and PrintWriter
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        try {
            // Write CSV header
            writer.write(CSV_HEADER);

            // Write transaction details into CSV format
            for (Transaction transaction : transactions) {
                writer.printf("%d,%s,%s,%.2f,%s,%s\n",
                        transaction.getTransactionId(),
                        transaction.getTransactionDate(),
                        transaction.getTransactionType(),
                        transaction.getTransactionAmount(),
                        transaction.getSenderAccount().getAccountNumber(),
                        transaction.getReceiverAccount() != null ? transaction.getReceiverAccount().getAccountNumber() : "N/A");
            }

            // Flush the writer to ensure all data is written to the StringWriter
            writer.flush();

            // Convert to ByteArrayInputStream and return
            return new ByteArrayInputStream(stringWriter.toString().getBytes());

        } finally {
            // Ensure writer is closed
            writer.close();
        }
    }
}

