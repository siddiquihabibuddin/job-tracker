package com.jobtracker.apps.domain.model;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "application_status_history",
        indexes = @Index(name = "idx_history_app_changed", columnList = "application_id, changed_at"))
public class ApplicationStatusHistory {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "application_id", nullable = false)
    private Application application;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "from_status", columnDefinition = "app_status")
    private AppStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(name = "to_status", nullable = false, columnDefinition = "app_status")
    private AppStatus toStatus;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt = OffsetDateTime.now();

    // getters/setters
    public Long getId() { return id; }
    public Application getApplication() { return application; }
    public void setApplication(Application application) { this.application = application; }
    public AppStatus getFromStatus() { return fromStatus; }
    public void setFromStatus(AppStatus fromStatus) { this.fromStatus = fromStatus; }
    public AppStatus getToStatus() { return toStatus; }
    public void setToStatus(AppStatus toStatus) { this.toStatus = toStatus; }
    public OffsetDateTime getChangedAt() { return changedAt; }
    public void setChangedAt(OffsetDateTime changedAt) { this.changedAt = changedAt; }
}