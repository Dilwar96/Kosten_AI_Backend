package com.kosten.ai.repository;

import com.kosten.ai.entity.Project;
import com.kosten.ai.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {
    List<Project> findByUser(User user);
    Page<Project> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    Optional<Project> findByIdAndUser(Long id, User user);
}
