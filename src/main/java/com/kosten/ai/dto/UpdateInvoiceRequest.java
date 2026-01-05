package com.kosten.ai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateInvoiceRequest {
    private String invoiceNumber;
    private String vendor;
    private BigDecimal amount;
    private String invoiceDate; // Format: YYYY-MM-DD
    private String description;
}
