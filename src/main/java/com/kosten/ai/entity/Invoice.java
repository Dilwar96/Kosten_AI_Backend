package com.kosten.ai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "invoices")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    @Column(nullable = false)
    private String invoiceNumber;
    
    @Column(nullable = false)
    private String vendor;
    
    @Column(nullable = false)
    private BigDecimal amount;
    
    private LocalDate invoiceDate;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
    
    private String fileName;
    
    @Column(length = 5000)
    private String aiExtractedData;
    
    @Column(name = "image_data")
    private byte[] imageData;
    
    private String contentType;
}
