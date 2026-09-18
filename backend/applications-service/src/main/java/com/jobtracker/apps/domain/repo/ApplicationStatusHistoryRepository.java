package com.jobtracker.apps.domain.repo;

import com.jobtracker.apps.domain.model.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {
    List<ApplicationStatusHistory> findAllByApplication_IdIn(Collection<UUID> applicationIds);
}