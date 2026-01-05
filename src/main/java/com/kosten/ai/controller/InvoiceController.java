package com.kosten.ai.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.kosten.ai.dto.InvoiceResponse;
import com.kosten.ai.dto.UpdateInvoiceRequest;
import com.kosten.ai.service.InvoiceService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping("/upload/{projectId}")
    public ResponseEntity<InvoiceResponse> uploadInvoice(@RequestParam("file") MultipartFile file,
                                                          @PathVariable Long projectId) {
        InvoiceResponse response = invoiceService.processInvoice(file, projectId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> getAllInvoices(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<InvoiceResponse> invoices = invoiceService.getUserInvoices(page, size);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<InvoiceResponse>> searchInvoices(
            @RequestParam(required = false) String invoiceNumber,
            @RequestParam(required = false) String vendor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Page<InvoiceResponse> invoices = invoiceService.searchInvoices(invoiceNumber, vendor, startDate, endDate, page, size);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<InvoiceResponse>> getProjectInvoices(@PathVariable Long projectId) {
        List<InvoiceResponse> invoices = invoiceService.getProjectInvoices(projectId);
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable Long id) {
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> downloadInvoiceImage(@PathVariable Long id) {
        byte[] imageData = invoiceService.getInvoiceImage(id);
        String contentType = invoiceService.getInvoiceContentType(id);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType != null ? contentType : "image/jpeg"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"invoice_" + id + ".jpg\"")
                .body(imageData);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(@PathVariable Long id, 
                                                          @RequestBody UpdateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteInvoice(@PathVariable Long id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok().build();
    }
}
