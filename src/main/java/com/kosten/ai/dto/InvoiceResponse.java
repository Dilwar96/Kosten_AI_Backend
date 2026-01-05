package com.kosten.ai.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class InvoiceResponse {
    private Long id;
    private Long projectId;
    private String projectName;
    private String invoiceNumber;
    private String vendor;
    private BigDecimal amount;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate invoiceDate;
    
    private String description;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime uploadedAt;
    
    private String fileName;
}
