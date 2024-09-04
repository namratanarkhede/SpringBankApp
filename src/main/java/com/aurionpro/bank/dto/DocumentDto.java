package com.aurionpro.bank.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
public class DocumentDto {
    private MultipartFile file;
    private String documentType;
}
