package com.jobtracker.apps.domain.repo;

import com.jobtracker.apps.domain.model.Application;
import com.jobtracker.apps.domain.model.AppStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface ApplicationRepository extends JpaRepository<Application, UUID>, JpaSpecificationExecutor<Application> {
    Page<Application> findAllByUser_IdAndDeletedAtIsNull(UUID userId, Pageable pageable);
    Page<Application> findAllByUser_IdAndStatusAndDeletedAtIsNull(UUID userId, AppStatus status, Pageable pageable);
}