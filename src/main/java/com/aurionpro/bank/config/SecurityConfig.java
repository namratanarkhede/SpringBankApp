package com.aurionpro.bank.config;

import static org.springframework.security.config.http.SessionCreationPolicy.STATELESS;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
//
//import com.aurionpro.bank.security.CustomerUserDetailsService;
import com.aurionpro.bank.security.JwtAuthenticationEntryPoint;
import com.aurionpro.bank.security.JwtAuthenticationFilter;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

//    @Autowired
//    private CustomerUserDetailsService customerUserDetailsService;

    @Autowired
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Autowired
    private JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public static PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(STATELESS))
            .authorizeHttpRequests(authz -> authz
                .requestMatchers("/api/auth/register").permitAll()
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/api/customers/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/admin/customer").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/admin/account").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/customers").hasRole("ADMIN")
                .requestMatchers(HttpMethod.GET, "/api/admin/transactions").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PATCH, "/api/admin/updatekycstatus/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.PUT, "/api/customers/updateProfile").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/customers/transactions").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.POST, "/api/customers/uploadDocument").hasRole("CUSTOMER")
                .requestMatchers(HttpMethod.GET, "/api/customers/transactions").hasRole("CUSTOMER")
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception.authenticationEntryPoint(jwtAuthenticationEntryPoint))
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
