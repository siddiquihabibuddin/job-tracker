package com.jobtracker.apps.domain.repo;

import com.jobtracker.apps.domain.model.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, Long> {}