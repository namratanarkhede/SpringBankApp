package com.aurionpro.bank.service;

import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.UserDto;

public interface AuthService {
    String registerAdmin(UserDto userDto);
    JwtAuthResponse login(LoginDto loginDto);
}
