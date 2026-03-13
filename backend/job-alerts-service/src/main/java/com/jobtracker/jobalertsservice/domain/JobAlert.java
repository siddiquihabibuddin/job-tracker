package com.jobtracker.jobalertsservice.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "job_alerts")
public class JobAlert extends Auditable {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "board_token")
    private String boardToken;

    @Column(name = "role_keywords", nullable = false)
    private String roleKeywords;

    @Column(name = "platforms", nullable = false)
    private String platforms;

    @Column(name = "workday_tenant")
    private String workdayTenant;

    @Column(name = "workday_site")
    private String workdaySite;

    @Column(name = "workday_wd_num")
    private Short workdayWdNum;

    @OneToMany(mappedBy = "alert", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("displayOrder ASC")
    private List<AlertCompany> companies = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getBoardToken() { return boardToken; }
    public void setBoardToken(String boardToken) { this.boardToken = boardToken; }
    public String getRoleKeywords() { return roleKeywords; }
    public void setRoleKeywords(String roleKeywords) { this.roleKeywords = roleKeywords; }
    public String getPlatforms() { return platforms; }
    public void setPlatforms(String platforms) { this.platforms = platforms; }
    public String getWorkdayTenant() { return workdayTenant; }
    public void setWorkdayTenant(String workdayTenant) { this.workdayTenant = workdayTenant; }
    public String getWorkdaySite() { return workdaySite; }
    public void setWorkdaySite(String workdaySite) { this.workdaySite = workdaySite; }
    public Short getWorkdayWdNum() { return workdayWdNum; }
    public void setWorkdayWdNum(Short workdayWdNum) { this.workdayWdNum = workdayWdNum; }
    public List<AlertCompany> getCompanies() { return companies; }
    public void setCompanies(List<AlertCompany> companies) { this.companies = companies; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public OffsetDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(OffsetDateTime deletedAt) { this.deletedAt = deletedAt; }
}
