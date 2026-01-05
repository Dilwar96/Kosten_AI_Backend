package com.kosten.ai.repository;

import com.kosten.ai.entity.Invoice;
import com.kosten.ai.entity.Project;
import com.kosten.ai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByUser(User user);
    Page<Invoice> findByUserOrderByUploadedAtDesc(User user, Pageable pageable);
    List<Invoice> findByProjectOrderByUploadedAtDesc(Project project);
    
    // Search by invoice number
    List<Invoice> findByUserAndInvoiceNumberContainingIgnoreCaseOrderByUploadedAtDesc(User user, String invoiceNumber);
    
    // Search by vendor
    List<Invoice> findByUserAndVendorContainingIgnoreCaseOrderByUploadedAtDesc(User user, String vendor);
    
    // Search by date range
    List<Invoice> findByUserAndInvoiceDateBetweenOrderByUploadedAtDesc(User user, LocalDate startDate, LocalDate endDate);
    
    // Combined search with pagination
    @Query("SELECT i FROM Invoice i WHERE i.user = :user " +
           "AND (COALESCE(:invoiceNumber, '') = '' OR LOWER(i.invoiceNumber) LIKE LOWER(CONCAT('%', :invoiceNumber, '%'))) " +
           "AND (COALESCE(:vendor, '') = '' OR LOWER(i.vendor) LIKE LOWER(CONCAT('%', :vendor, '%'))) " +
           "AND (:startDate IS NULL OR i.invoiceDate >= :startDate) " +
           "AND (:endDate IS NULL OR i.invoiceDate <= :endDate) " +
           "ORDER BY i.uploadedAt DESC")
    Page<Invoice> searchInvoices(@Param("user") User user, 
                                  @Param("invoiceNumber") String invoiceNumber,
                                  @Param("vendor") String vendor,
                                  @Param("startDate") LocalDate startDate,
                                  @Param("endDate") LocalDate endDate,
                                  Pageable pageable);
}

