package com.aurionpro.bank.service;


import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.entity.Account;
import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;
import com.aurionpro.bank.entity.Transaction;
import com.aurionpro.bank.enums.AccountStatus;
import com.aurionpro.bank.enums.DocumentType;
import com.aurionpro.bank.enums.KycStatus;
import com.aurionpro.bank.enums.TransactionType;
import com.aurionpro.bank.exception.CustomerServiceException;
import com.aurionpro.bank.exception.UserApiException;
import com.aurionpro.bank.repo.AccountRepo;
import com.aurionpro.bank.repo.CustomerRepo;
import com.aurionpro.bank.repo.DocumentRepo;
import com.aurionpro.bank.repo.TransactionRepo;
import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;


@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerRepo customerRepo;

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
        
        // Fetch customer by email
        Customer customer = customerRepo.findByEmail(username)
                .orElseThrow(() -> new CustomerServiceException("Customer not found"));
        
        // Validate password
        if (!passwordEncoder.matches(password, customer.getPassword())) {
            logger.warn("Invalid password for username: {}", username);
            throw new CustomerServiceException("Invalid password");
        }
        
        // Successful validation
        logger.info("Customer login validated successfully for username: {}", username);
        return new CustomerDto(
            customer.getCustomerId(),
            customer.getFirstName(),
            customer.getLastName(),
            customer.getEmail(),
            customer.getDateOfBirth(),
            null,
            null
        );
    }
    @Override
    public CustomerDto updateCustomerProfile(String username, CustomerDto customerDto) {
        logger.info("Updating profile for customer: {}", username);

        // Fetch the existing customer
        Customer customer = customerRepo.findByEmail(username)
            .orElseThrow(() -> new UserApiException(HttpStatus.NOT_FOUND, "Customer not found"));

        // Verify current password if provided
        if (customerDto.getCurrentPassword() != null && !customerDto.getCurrentPassword().isEmpty()) {
            if (!passwordEncoder.matches(customerDto.getCurrentPassword(), customer.getPassword())) {
                throw new UserApiException(HttpStatus.NOT_FOUND, "Current password is incorrect");
            }
            // Update password if new password is provided
            if (customerDto.getNewPassword() != null && !customerDto.getNewPassword().isEmpty()) {
                customer.setPassword(passwordEncoder.encode(customerDto.getNewPassword()));
            }
        }

        // Update profile details
        customer.setFirstName(customerDto.getFirstName());
        customer.setLastName(customerDto.getLastName());
        customer.setEmail(customerDto.getEmail());
        customer.setDateOfBirth(customerDto.getDateOfBirth());

        // Save updated customer to the repository
        customerRepo.save(customer);

        logger.info("Profile updated successfully for customer: {}", username);

        // Convert and return updated CustomerDto
        return new CustomerDto(
                customer.getCustomerId(),
                customer.getFirstName(),
                customer.getLastName(),
                customer.getEmail(),
                customer.getDateOfBirth(),
                null, // Current password (not included in response)
                null  // New password (not included in response)
        );
    }
    @Override
    public void performTransaction(String username, TransactionDto transactionDto) {
        logger.info("Performing transaction for customer: {}, TransactionType: {}", username, transactionDto.getTransactionType());

        Customer customer = findCustomerByUsername(username);
        Account senderAccount = findAccountByNumber(transactionDto.getSenderAccountNumber());

        validateAccountOwnership(senderAccount, customer);

        // Check if the account is active
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

        // Send transaction notification email
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

        // Ensure no receiver account is provided for credit transactions
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

        // Ensure no receiver account is provided for debit transactions
        if (transactionDto.getReceiverAccountNumber() != null) {
            logger.warn("Receiver account number should not be provided for debit transactions. Provided receiver account: {}", transactionDto.getReceiverAccountNumber());
            throw new CustomerServiceException("Receiver account should not be provided for debit transactions");
        }

        if (account.getBalance() < transactionAmount) {
            logger.warn("Insufficient balance for debit. Account: {}, Available balance: {}", 
                        account.getAccountNumber(), account.getBalance());
            throw new CustomerServiceException("Insufficient balance");
        }

        account.setBalance(account.getBalance() - transactionAmount);
        accountRepo.save(account);

        saveTransaction(account, null, transactionAmount, transactionDto);
    }


    private void updateAccountBalances(Account senderAccount, Account receiverAccount, double transactionAmount) {
        logger.debug("Updating account balances for sender account: {}, receiver account: {}, amount: {}", 
                     senderAccount.getAccountNumber(), receiverAccount.getAccountNumber(), transactionAmount);
        senderAccount.setBalance(senderAccount.getBalance() - transactionAmount);
        receiverAccount.setBalance(receiverAccount.getBalance() + transactionAmount);
        accountRepo.save(senderAccount);
        accountRepo.save(receiverAccount);
    }

    private void saveTransaction(Account senderAccount, Account receiverAccount, double transactionAmount, TransactionDto transactionDto) {
        logger.debug("Saving transaction. Sender account: {}, Receiver account: {}, amount: {}", 
                     senderAccount.getAccountNumber(), 
                     receiverAccount != null ? receiverAccount.getAccountNumber() : "N/A", 
                     transactionAmount);
        Transaction transaction = new Transaction();
        transaction.setTransactionAmount(transactionAmount);
        transaction.setTransactionDate(transactionDto.getTransactionDate());
        transaction.setTransactionType(transactionDto.getTransactionType());
        transaction.setSenderAccount(senderAccount);
        transaction.setReceiverAccount(receiverAccount);
        transactionRepo.save(transaction);
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
    public String uploadDocument(Integer customerId, MultipartFile file, DocumentType documentType) {
        try {
            Customer customer = customerRepo.findById(customerId)
                    .orElseThrow(() -> new RuntimeException("Customer not found"));

            // Upload the document to Cloudinary
            Map<String, Object> uploadResult = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.emptyMap());
            String documentUrl = (String) uploadResult.get("url");

            // Save the document details
            Document document = new Document();
            document.setDocumentType(documentType);
            document.setKycStatus(KycStatus.PENDING);
            document.setDocumentUrl(documentUrl);
            document.setCustomer(customer);

            documentRepo.save(document);

            return "Document uploaded successfully!";
        } catch (IOException e) {
            throw new RuntimeException("Failed to upload document", e);
        }
    }
}
