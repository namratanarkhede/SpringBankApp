package com.aurionpro.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtAuthResponse {
	
	private String accessToken;
	
	private String tokenType = "Bearer";

	// Constructor that takes only the token
    public JwtAuthResponse(String accessToken) {
        this.accessToken = accessToken;
    }
}
