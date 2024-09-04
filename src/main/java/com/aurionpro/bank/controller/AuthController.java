package com.aurionpro.bank.controller;

import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.UserDto;
import com.aurionpro.bank.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> registerAdmin(@RequestBody UserDto userDto) {
        String response = authService.registerAdmin(userDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginDto loginDto) {
        JwtAuthResponse jwtResponse = authService.login(loginDto);
        return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
    }
}
