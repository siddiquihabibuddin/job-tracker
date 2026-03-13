package com.jobtracker.jobalertsservice.repository;

import com.jobtracker.jobalertsservice.domain.AlertCompany;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertCompanyRepository extends JpaRepository<AlertCompany, UUID> {
    void deleteByAlert_Id(UUID alertId);
}
