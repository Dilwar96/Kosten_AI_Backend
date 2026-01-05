package com.kosten.ai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kosten.ai.dto.InvoiceResponse;
import com.kosten.ai.dto.UpdateInvoiceRequest;
import com.kosten.ai.entity.Invoice;
import com.kosten.ai.entity.Project;
import com.kosten.ai.entity.User;
import com.kosten.ai.exception.AiServiceException;
import com.kosten.ai.exception.FileProcessingException;
import com.kosten.ai.exception.InvalidRequestException;
import com.kosten.ai.exception.ResourceNotFoundException;
import com.kosten.ai.exception.UnauthorizedException;
import com.kosten.ai.repository.InvoiceRepository;
import com.kosten.ai.repository.ProjectRepository;
import com.kosten.ai.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final GeminiAiService geminiAiService;
    private final ObjectMapper objectMapper;

    public InvoiceResponse processInvoice(MultipartFile file, Long projectId) {
        try {
            if (file.isEmpty()) {
                throw new InvalidRequestException("Uploaded file is empty");
            }
            
            String contentType = file.getContentType();
            if (contentType == null || (!contentType.startsWith("image/") && !contentType.equals("application/pdf"))) {
                throw new InvalidRequestException("File must be an image (JPEG, PNG) or PDF");
            }
            
            String username = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

            // Optional: Find and validate project
            Project project = null;
            if (projectId != null) {
                project = projectRepository.findByIdAndUser(projectId, user)
                        .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));
            }

            // Convert image to base64
            byte[] fileBytes = file.getBytes();
            String base64Image = Base64.getEncoder().encodeToString(fileBytes);

            // Extract data using Gemini AI
            String aiResponse = geminiAiService.extractInvoiceData(base64Image);
            
            if (aiResponse.startsWith("Fehler")) {
                throw new AiServiceException("Failed to extract invoice data: " + aiResponse);
            }

            // Parse AI response
            Invoice invoice = parseAiResponse(aiResponse);
            invoice.setUser(user);
            invoice.setProject(project);
            invoice.setFileName(file.getOriginalFilename());
            invoice.setAiExtractedData(aiResponse);
            invoice.setImageData(fileBytes);
            invoice.setContentType(file.getContentType());

            invoice = invoiceRepository.save(invoice);

            return convertToResponse(invoice);

        } catch (ResourceNotFoundException | InvalidRequestException | AiServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new FileProcessingException("Error processing invoice file: " + e.getMessage(), e);
        }
    }

    public Page<InvoiceResponse> getUserInvoices(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidRequestException("Page must be >= 0 and size must be > 0");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Pageable pageable = PageRequest.of(page, size);
        return invoiceRepository.findByUserOrderByUploadedAtDesc(user, pageable)
                .map(this::convertToResponse);
    }

    public List<InvoiceResponse> getProjectInvoices(Long projectId) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Project project = projectRepository.findByIdAndUser(projectId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Project", projectId));

        return invoiceRepository.findByProjectOrderByUploadedAtDesc(project)
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public Page<InvoiceResponse> searchInvoices(String invoiceNumber, String vendor, LocalDate startDate, LocalDate endDate, int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidRequestException("Page must be >= 0 and size must be > 0");
        }
        
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new InvalidRequestException("Start date must be before or equal to end date");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Pageable pageable = PageRequest.of(page, size);
        return invoiceRepository.searchInvoices(user, invoiceNumber, vendor, startDate, endDate, pageable)
                .map(this::convertToResponse);
    }

    public InvoiceResponse getInvoiceById(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to access this invoice");
        }

        return convertToResponse(invoice);
    }

    public byte[] getInvoiceImage(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to access this invoice");
        }

        if (invoice.getImageData() == null) {
            throw new ResourceNotFoundException("Invoice image data not found");
        }

        return invoice.getImageData();
    }

    public String getInvoiceContentType(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to access this invoice");
        }

        return invoice.getContentType();
    }

    public InvoiceResponse updateInvoice(Long id, UpdateInvoiceRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to update this invoice");
        }

        // Update nur die Felder die gesetzt sind
        if (request.getInvoiceNumber() != null) {
            invoice.setInvoiceNumber(request.getInvoiceNumber());
        }
        if (request.getVendor() != null) {
            invoice.setVendor(request.getVendor());
        }
        if (request.getAmount() != null) {
            if (request.getAmount().compareTo(BigDecimal.ZERO) < 0) {
                throw new InvalidRequestException("Amount cannot be negative");
            }
            invoice.setAmount(request.getAmount());
        }
        if (request.getInvoiceDate() != null) {
            try {
                LocalDate date = LocalDate.parse(request.getInvoiceDate());
                if (date.isAfter(LocalDate.now())) {
                    throw new InvalidRequestException("Invoice date cannot be in the future");
                }
                invoice.setInvoiceDate(date);
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidRequestException("Invalid date format. Use YYYY-MM-DD");
            }
        }
        if (request.getDescription() != null) {
            invoice.setDescription(request.getDescription());
        }

        invoice = invoiceRepository.save(invoice);
        return convertToResponse(invoice);
    }

    public void deleteInvoice(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You are not authorized to delete this invoice");
        }

        invoiceRepository.delete(invoice);
    }

    private Invoice parseAiResponse(String aiResponse) {
        Invoice invoice = new Invoice();
        
        try {
            // Remove markdown code blocks if present
            String cleanedResponse = aiResponse.trim();
            if (cleanedResponse.startsWith("```json")) {
                cleanedResponse = cleanedResponse.substring(7);
            }
            if (cleanedResponse.startsWith("```")) {
                cleanedResponse = cleanedResponse.substring(3);
            }
            if (cleanedResponse.endsWith("```")) {
                cleanedResponse = cleanedResponse.substring(0, cleanedResponse.length() - 3);
            }
            cleanedResponse = cleanedResponse.trim();
            
            // Try to parse JSON response
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);
            
            invoice.setInvoiceNumber(jsonNode.has("invoiceNumber") ? 
                jsonNode.get("invoiceNumber").asText() : "Unbekannt");
            invoice.setVendor(jsonNode.has("vendor") ? 
                jsonNode.get("vendor").asText() : "Unbekannt");
            
            // Parse amount
            if (jsonNode.has("amount")) {
                String amountStr = jsonNode.get("amount").asText().replaceAll("[^0-9.]", "");
                invoice.setAmount(!amountStr.isEmpty() ? new BigDecimal(amountStr) : BigDecimal.ZERO);
            } else {
                invoice.setAmount(BigDecimal.ZERO);
            }
            
            // Parse date
            if (jsonNode.has("date")) {
                try {
                    invoice.setInvoiceDate(LocalDate.parse(jsonNode.get("date").asText()));
                } catch (Exception e) {
                    invoice.setInvoiceDate(LocalDate.now());
                }
            } else {
                invoice.setInvoiceDate(LocalDate.now());
            }
            
            invoice.setDescription(jsonNode.has("description") ? 
                jsonNode.get("description").asText() : "Keine Beschreibung");
                
        } catch (Exception e) {
            // Fallback: save raw AI response in description
            invoice.setInvoiceNumber("Parsing fehlgeschlagen");
            invoice.setVendor("Unbekannt");
            invoice.setAmount(BigDecimal.ZERO);
            invoice.setInvoiceDate(LocalDate.now());
            invoice.setDescription("Raw AI Response: " + aiResponse);
        }
        
        return invoice;
    }

    private InvoiceResponse convertToResponse(Invoice invoice) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setProjectId(invoice.getProject() != null ? invoice.getProject().getId() : null);
        response.setProjectName(invoice.getProject() != null ? invoice.getProject().getName() : null);
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setVendor(invoice.getVendor());
        response.setAmount(invoice.getAmount());
        response.setInvoiceDate(invoice.getInvoiceDate());
        response.setDescription(invoice.getDescription());
        response.setUploadedAt(invoice.getUploadedAt());
        response.setFileName(invoice.getFileName());
        return response;
    }
}
