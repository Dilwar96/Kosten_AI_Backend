package com.kosten.ai.service;

import com.kosten.ai.dto.CreateProjectRequest;
import com.kosten.ai.dto.ProjectResponse;
import com.kosten.ai.entity.Project;
import com.kosten.ai.entity.User;
import com.kosten.ai.exception.InvalidRequestException;
import com.kosten.ai.exception.ResourceNotFoundException;
import com.kosten.ai.exception.UnauthorizedException;
import com.kosten.ai.repository.ProjectRepository;
import com.kosten.ai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final UserRepository userRepository;

    public ProjectResponse createProject(CreateProjectRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new InvalidRequestException("Project name cannot be empty");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Project project = new Project();
        project.setName(request.getName().trim());
        project.setDescription(request.getDescription());
        project.setUser(user);

        project = projectRepository.save(project);
        return convertToResponse(project);
    }

    public Page<ProjectResponse> getUserProjects(int page, int size) {
        if (page < 0 || size <= 0) {
            throw new InvalidRequestException("Page must be >= 0 and size must be > 0");
        }
        
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Pageable pageable = PageRequest.of(page, size);
        return projectRepository.findByUserOrderByCreatedAtDesc(user, pageable)
                .map(this::convertToResponse);
    }

    public ProjectResponse getProjectById(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Project project = projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        return convertToResponse(project);
    }

    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Project project = projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        if (request.getName() != null) {
            if (request.getName().trim().isEmpty()) {
                throw new InvalidRequestException("Project name cannot be empty");
            }
            project.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }

        project = projectRepository.save(project);
        return convertToResponse(project);
    }

    public void deleteProject(Long id) {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

        Project project = projectRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResourceNotFoundException("Project", id));

        projectRepository.delete(project);
    }

    private ProjectResponse convertToResponse(Project project) {
        ProjectResponse response = new ProjectResponse();
        response.setId(project.getId());
        response.setName(project.getName());
        response.setDescription(project.getDescription());
        response.setCreatedAt(project.getCreatedAt());
        response.setInvoiceCount(project.getInvoices() != null ? project.getInvoices().size() : 0);
        
        // Calculate total cost
        BigDecimal totalCost = BigDecimal.ZERO;
        if (project.getInvoices() != null) {
            totalCost = project.getInvoices().stream()
                    .map(invoice -> invoice.getAmount() != null ? invoice.getAmount() : BigDecimal.ZERO)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }
        response.setTotalCost(totalCost);
        
        return response;
    }
}
