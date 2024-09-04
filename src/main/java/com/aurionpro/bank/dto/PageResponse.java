package com.aurionpro.bank.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private List<T> content;      
    private int pageNumber;     
    private int pageSize;         
    private long totalElements;   
    private int totalPages;      
    private boolean isLastPage;  
}
