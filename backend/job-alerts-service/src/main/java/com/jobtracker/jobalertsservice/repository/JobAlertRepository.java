package com.jobtracker.jobalertsservice.repository;

import com.jobtracker.jobalertsservice.domain.JobAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface JobAlertRepository extends JpaRepository<JobAlert, UUID> {
    List<JobAlert> findByUserIdAndDeletedAtIsNull(UUID userId);
    List<JobAlert> findByActiveIsTrueAndDeletedAtIsNull();
}
