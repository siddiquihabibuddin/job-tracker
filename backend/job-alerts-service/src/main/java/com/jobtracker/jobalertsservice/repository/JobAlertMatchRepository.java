package com.jobtracker.jobalertsservice.repository;

import com.jobtracker.jobalertsservice.domain.JobAlertMatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

public interface JobAlertMatchRepository extends JpaRepository<JobAlertMatch, UUID> {
    List<JobAlertMatch> findByAlert_IdOrderByCreatedAtDesc(UUID alertId);
    List<JobAlertMatch> findByUserIdAndSeenAtIsNullOrderByCreatedAtDesc(UUID userId);
    long countByUserIdAndSeenAtIsNull(UUID userId);
    boolean existsByAlert_IdAndPlatformAndExternalId(UUID alertId, String platform, String externalId);
    @Transactional
    void deleteByAlert_Id(UUID alertId);
}
