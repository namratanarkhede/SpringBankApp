package com.aurionpro.bank.service;

import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.UserDto;
import com.aurionpro.bank.entity.Role;
import com.aurionpro.bank.entity.User;
import com.aurionpro.bank.exception.UserApiException;
import com.aurionpro.bank.repo.RoleRepo;
import com.aurionpro.bank.repo.UserRepo;
import com.aurionpro.bank.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

    private final UserRepo userRepo;
    private final RoleRepo roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;

    @Override
    public String registerAdmin(UserDto userDto) {
        logger.info("Attempting to register admin with username: {}", userDto.getUsername());

        if (userRepo.existsByUsername(userDto.getUsername())) {
            logger.error("Username {} is already taken", userDto.getUsername());
            throw new UserApiException(HttpStatus.BAD_REQUEST, "Username is already taken!");
        }

        User user = new User();
        user.setUsername(userDto.getUsername());
        user.setPassword(passwordEncoder.encode(userDto.getPassword()));

        Role roles = roleRepo.findByRoleName("ROLE_ADMIN")
                .orElseThrow(() -> {
                    logger.error("Role ROLE_ADMIN not found");
                    return new UserApiException(HttpStatus.BAD_REQUEST, "Role not found.");
                });
        user.setRoles(Collections.singleton(roles));

        userRepo.save(user);
        logger.info("Admin registered successfully with username: {}", userDto.getUsername());
        return "Admin registered successfully!";
    }

    @Override
    public JwtAuthResponse login(LoginDto loginDto) {
        logger.info("Attempting login for username: {}", loginDto.getUsername());

        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            loginDto.getUsername(),
                            loginDto.getPassword()
                    )
            );
        } catch (Exception e) {
            logger.error("Login attempt failed for username: {}", loginDto.getUsername(), e);
            throw new UserApiException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtTokenProvider.generateToken(authentication);

        logger.info("Login successful for username: {}", loginDto.getUsername());
        // Return the JWT token wrapped in JwtAuthResponse
        return new JwtAuthResponse(token);
    }
}
