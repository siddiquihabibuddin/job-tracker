package com.jobtracker.apps.domain.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "applications",
        indexes = {
                @Index(name = "idx_apps_user_created", columnList = "user_id, created_at DESC"),
                @Index(name = "idx_apps_user_status_created", columnList = "user_id, status, created_at DESC"),
                @Index(name = "idx_apps_user_source_created", columnList = "user_id, source, created_at DESC")
        })
public class Application extends Auditable {
    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false) private String company;
    @Column(nullable = false) private String role;

    @Enumerated(EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.NAMED_ENUM)
    @Column(nullable = false, columnDefinition = "app_status")
    private AppStatus status = AppStatus.APPLIED;

    private String source;
    private String location;

    @Column(name = "salary_min") private BigDecimal salaryMin;
    @Column(name = "salary_max") private BigDecimal salaryMax;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.CHAR)
    @Column(name = "currency", length = 3, nullable = false)
    private String currency = "USD";

    @Column(name = "next_follow_up_on") private LocalDate nextFollowUpOn;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "tags_json", columnDefinition = "jsonb")
    private String tagsJson;
    @Column(name = "deleted_at") private java.time.OffsetDateTime deletedAt;

    @Column(name = "applied_at") private LocalDate appliedAt;
    @Column(name = "job_link") private String jobLink;
    @Column(name = "resume_uploaded") private String resumeUploaded;
    @Column(name = "got_call") private boolean gotCall;
    @Column(name = "reject_date") private LocalDate rejectDate;
    @Column(name = "login_details") private String loginDetails;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public AppStatus getStatus() { return status; }
    public void setStatus(AppStatus status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public BigDecimal getSalaryMin() { return salaryMin; }
    public void setSalaryMin(BigDecimal salaryMin) { this.salaryMin = salaryMin; }
    public BigDecimal getSalaryMax() { return salaryMax; }
    public void setSalaryMax(BigDecimal salaryMax) { this.salaryMax = salaryMax; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public LocalDate getNextFollowUpOn() { return nextFollowUpOn; }
    public void setNextFollowUpOn(LocalDate nextFollowUpOn) { this.nextFollowUpOn = nextFollowUpOn; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public java.time.OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(java.time.OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
    public LocalDate getAppliedAt() { return appliedAt; }
    public void setAppliedAt(LocalDate appliedAt) { this.appliedAt = appliedAt; }
    public String getJobLink() { return jobLink; }
    public void setJobLink(String jobLink) { this.jobLink = jobLink; }
    public String getResumeUploaded() { return resumeUploaded; }
    public void setResumeUploaded(String resumeUploaded) { this.resumeUploaded = resumeUploaded; }
    public boolean isGotCall() { return gotCall; }
    public void setGotCall(boolean gotCall) { this.gotCall = gotCall; }
    public LocalDate getRejectDate() { return rejectDate; }
    public void setRejectDate(LocalDate rejectDate) { this.rejectDate = rejectDate; }
    public String getLoginDetails() { return loginDetails; }
    public void setLoginDetails(String loginDetails) { this.loginDetails = loginDetails; }
}