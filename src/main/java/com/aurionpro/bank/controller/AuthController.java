//package com.aurionpro.bank.controller;
//
//import com.aurionpro.bank.dto.JwtAuthResponse;
//import com.aurionpro.bank.dto.LoginDto;
//import com.aurionpro.bank.dto.UserDto;
//import com.aurionpro.bank.service.AuthService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/auth")
//@RequiredArgsConstructor
//public class AuthController {
//
//    private final AuthService authService;
//
//    @PostMapping("/register")
//    public ResponseEntity<String> registerAdmin(@RequestBody UserDto userDto) {
//        String response = authService.registerAdmin(userDto);
//        return new ResponseEntity<>(response, HttpStatus.CREATED);
//    }
//
//    @PostMapping("/login")
//    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginDto loginDto) {
//        JwtAuthResponse jwtResponse = authService.login(loginDto);
//        return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
//    }
//}
package com.aurionpro.bank.controller;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import com.aurionpro.bank.dto.JwtAuthResponse;
import com.aurionpro.bank.dto.LoginDto;
import com.aurionpro.bank.dto.UserDto;
import com.aurionpro.bank.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.google.code.kaptcha.impl.DefaultKaptcha;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Autowired
    private DefaultKaptcha defaultKaptcha;

    @PostMapping("/register")
    public ResponseEntity<String> registerAdmin(@RequestBody UserDto userDto) {
        String response = authService.registerAdmin(userDto);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/captcha")
    public void getCaptcha(HttpServletResponse response, HttpServletRequest request) throws IOException {
        String text = defaultKaptcha.createText();
        BufferedImage image = defaultKaptcha.createImage(text);

        request.getSession().setAttribute("captcha", text);

        response.setContentType("image/jpeg");
        OutputStream outputStream = response.getOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        outputStream.close();
    }

    public boolean verifyCaptcha(HttpServletRequest request, String captcha) {
        String captchaSession = (String) request.getSession().getAttribute("captcha");
        return captcha.equals(captchaSession);
    }

    @PostMapping("/login")
    public ResponseEntity<JwtAuthResponse> login(@RequestBody LoginDto loginDto, HttpServletRequest request) {
        if (!verifyCaptcha(request, loginDto.getCaptcha())) {
            throw new IllegalArgumentException("Invalid CAPTCHA");
        }

        JwtAuthResponse jwtResponse = authService.login(loginDto);
        return new ResponseEntity<>(jwtResponse, HttpStatus.OK);
    }
}
