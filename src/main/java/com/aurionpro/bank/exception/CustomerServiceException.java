package com.aurionpro.bank.exception;

import java.io.IOException;

public class CustomerServiceException extends RuntimeException {
    public CustomerServiceException(String message) {
        super(message);
    }

	public CustomerServiceException(String string, IOException e) {
		// TODO Auto-generated constructor stub
	}
}
