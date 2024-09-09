package com.aurionpro.bank.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.aurionpro.bank.dto.AccountDto;
import com.aurionpro.bank.dto.CustomerDto;
import com.aurionpro.bank.dto.PageResponse;
import com.aurionpro.bank.dto.TransactionDto;
import com.aurionpro.bank.entity.Account;
import com.aurionpro.bank.entity.Bank;
import com.aurionpro.bank.entity.Customer;
import com.aurionpro.bank.entity.Document;
import com.aurionpro.bank.entity.Role;
import com.aurionpro.bank.entity.Transaction;
import com.aurionpro.bank.entity.User;
import com.aurionpro.bank.enums.AccountStatus;
import com.aurionpro.bank.enums.KycStatus;
import com.aurionpro.bank.exception.CustomerServiceException;
import com.aurionpro.bank.exception.UserApiException;
import com.aurionpro.bank.repo.AccountRepo;
import com.aurionpro.bank.repo.BankRepo;
import com.aurionpro.bank.repo.CustomerRepo;
import com.aurionpro.bank.repo.DocumentRepo;
import com.aurionpro.bank.repo.RoleRepo;
import com.aurionpro.bank.repo.TransactionRepo;
import com.aurionpro.bank.repo.UserRepo;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private static final Logger logger = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final AccountRepo accountRepo;
    private final TransactionRepo transactionRepo;
    private final BankRepo bankRepo;
    private final RoleRepo roleRepo;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final CustomerRepo customerRepo;
    private final MailService mailService;
    @Autowired
    private DocumentRepo documentRepo;
	
	@Override
	public String addCustomer(CustomerDto customerDto) {
	    logger.info("Adding a new customer with email: {}", customerDto.getEmail());
	
	    if (customerRepo.findByEmail(customerDto.getEmail()).isPresent()) {
	        logger.warn("Customer with email {} already exists.", customerDto.getEmail());
	        throw new UserApiException(HttpStatus.BAD_REQUEST, "Customer with this email already exists.");
	    }
	
	    // Extract date part from date of birth
	    LocalDate dateOfBirth = customerDto.getDateOfBirth();
	    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd"); // Format to get the day of month
	    String formattedDate = dateOfBirth.format(formatter);
	
	    // Generate password based on first name and formatted date of birth
	    String generatedPassword = customerDto.getFirstName().toLowerCase() + "@" + formattedDate;
	
	    Customer customer = new Customer();
	    customer.setFirstName(customerDto.getFirstName());
	    customer.setLastName(customerDto.getLastName());
	    customer.setEmail(customerDto.getEmail());
	    customer.setPassword(passwordEncoder.encode(generatedPassword)); // Encode generated password
	    customer.setDateOfBirth(customerDto.getDateOfBirth());
	
	    // Set role for the customer
	    Role customerRole = roleRepo.findByRoleName("ROLE_CUSTOMER")
	            .orElseThrow(() -> new UserApiException(HttpStatus.BAD_REQUEST, "Role not found."));
	
	    customerRepo.save(customer);
	    logger.info("Customer saved successfully with email: {}", customerDto.getEmail());
	
	    // Save to User table
	    User user = new User();
	    user.setUsername(customer.getEmail());
	    user.setPassword(passwordEncoder.encode(generatedPassword)); // Encode generated password
	    user.setRoles(Collections.singleton(customerRole));
	
	    userRepo.save(user);
	    logger.info("User saved successfully with username: {}", customer.getEmail());
	
	    return "Customer added successfully!";
	}
	    
	@Override
	public String addAccount(AccountDto accountDto) {
	    logger.info("Adding a new account for customer ID: {}", accountDto.getCustomerId());

	    // Retrieve customer and bank from repositories
	    Customer customer = customerRepo.findById(accountDto.getCustomerId())
	            .orElseThrow(() -> new UserApiException(HttpStatus.BAD_REQUEST, "Customer not found."));

	    Bank bank = bankRepo.findById(accountDto.getBankId())
	            .orElseThrow(() -> new UserApiException(HttpStatus.BAD_REQUEST, "Bank not found."));

	    // Create a new account
	    Account account = new Account();
	    account.setAccountNumber(generateAccountNumber());
	    account.setBalance(0.0); // Initialize balance to 0.0
	    account.setStatus(AccountStatus.ACTIVE); // Set status to ACTIVE
	    account.setCustomer(customer);
	    account.setBank(bank);

	    // Save the account to the repository
	    accountRepo.save(account);
	    logger.info("Account added successfully with account number: {}", account.getAccountNumber());

	    // Prepare account details for email
	    String fullName = customer.getFirstName() + " " + customer.getLastName();
	    String passwordDescription = "Your password is formatted as '<FirstName>@<DayOfBirth>'. For example, if your name is John and your birth date is January 1st, your password will be 'john@01'.";
	    String accountNumber = account.getAccountNumber();
	    double balance = account.getBalance();
	    String email = customer.getEmail();

	    // Send email notification to the customer
	    mailService.sendAccountCreationEmail(email, fullName, passwordDescription, accountNumber, balance);

	    return "Account added successfully!";
	}


    @Override
    public PageResponse<Customer> viewCustomers(Pageable pageable) {
        logger.info("Fetching customers with pagination - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Customer> customerPage = customerRepo.findAll(pageable);
        logger.debug("Fetched {} customers", customerPage.getNumberOfElements());
        return new PageResponse<>(
                customerPage.getContent(),
                customerPage.getNumber(),
                customerPage.getSize(),
                customerPage.getTotalElements(),
                customerPage.getTotalPages(),
                customerPage.isLast()
        );
    }

    @Override
    public PageResponse<TransactionDto> viewAllTransactions(Pageable pageable) {
        logger.info("Fetching transactions with pagination - page: {}, size: {}", pageable.getPageNumber(), pageable.getPageSize());
        Page<Transaction> transactionPage = transactionRepo.findAll(pageable);
        logger.debug("Fetched {} transactions", transactionPage.getNumberOfElements());

        // Convert Transaction entities to TransactionDto
        List<TransactionDto> transactionDtos = transactionPage.getContent().stream()
                .map(transaction -> {
                    TransactionDto dto = new TransactionDto();
                    dto.setTransactionId(transaction.getTransactionId());
                    dto.setTransactionDate(transaction.getTransactionDate());
                    dto.setTransactionAmount(transaction.getTransactionAmount());
                    dto.setTransactionType(transaction.getTransactionType());
                    
                    // Handle the case where sender and receiver accounts might be null
                    dto.setSenderAccountNumber(transaction.getSenderAccount() != null 
                            ? transaction.getSenderAccount().getAccountNumber() 
                            : null);
                    
                    dto.setReceiverAccountNumber(transaction.getReceiverAccount() != null 
                            ? transaction.getReceiverAccount().getAccountNumber() 
                            : null);
                    
                    return dto;
                })
                .collect(Collectors.toList());

        return new PageResponse<>(
                transactionDtos,
                transactionPage.getNumber(),
                transactionPage.getSize(),
                transactionPage.getTotalElements(),
                transactionPage.getTotalPages(),
                transactionPage.isLast()
        );
    }

    @Override
    public String addBank(Bank bank) {
        logger.info("Adding a new bank with name: {}", bank.getBankName());
        bankRepo.save(bank);
        logger.info("Bank added successfully with name: {}", bank.getBankName());
        return "Bank added successfully!";
    }
    
    @Override
    public String deleteCustomer(int customerId) {
        logger.info("Deactivating customer with ID: {}", customerId);
        
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new UserApiException(HttpStatus.BAD_REQUEST, "Customer not found."));

        // Retrieve all accounts associated with this customer
        List<Account> accounts = accountRepo.findByCustomer(customer);

        if (accounts.isEmpty()) {
            logger.warn("No accounts found for customer with ID: {}", customerId);
            throw new UserApiException(HttpStatus.BAD_REQUEST, "No accounts found for the customer.");
        }

        // Set the status of all associated accounts to INACTIVE
        for (Account account : accounts) {
            account.setStatus(AccountStatus.INACTIVE);
            accountRepo.save(account);
            logger.info("Account with number {} set to INACTIVE.", account.getAccountNumber());
        }

        // Optionally, you can also deactivate the customer if needed
        // customer.setStatus(CustomerStatus.INACTIVE);
        // customerRepo.save(customer);
        // logger.info("Customer with ID {} set to INACTIVE.", customerId);

        return "Customer deactivated successfully!";
    }

    private String generateAccountNumber() {
        logger.debug("Generating a new account number.");
        SecureRandom random = new SecureRandom();
        long number = random.nextLong(1000000000L, 9999999999L);  // Generate a 10-digit number
        String accountNumber = String.format("%010d", number); // Format as a 10-digit string
        logger.debug("Generated account number: {}", accountNumber);
        return accountNumber;
    }
    
    

    @Override
    public void verifyDocument(Long documentId, KycStatus kycStatus) {
        Document document = documentRepo.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found"));

        document.setKycStatus(kycStatus);
        documentRepo.save(document);
    }

    @Override
    public List<Document> getDocumentsByCustomerId(int customerId) {
        // Fetch the customer by customer ID
        Customer customer = customerRepo.findById(customerId)
                .orElseThrow(() -> new CustomerServiceException("Customer not found with ID: " + customerId));

        // Fetch the documents by customer
        List<Document> documents = documentRepo.findByCustomer(customer);
        
        // Check if the customer has any documents
        if (documents.isEmpty()) {
            throw new CustomerServiceException("No documents found for the customer with ID: " + customerId);
        }

        return documents;
    }

}

