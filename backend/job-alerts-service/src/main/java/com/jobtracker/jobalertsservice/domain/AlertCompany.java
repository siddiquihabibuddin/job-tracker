package com.jobtracker.jobalertsservice.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "alert_companies")
public class AlertCompany {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    private JobAlert alert;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "board_token")
    private String boardToken;

    @Column(name = "workday_tenant")
    private String workdayTenant;

    @Column(name = "workday_site")
    private String workdaySite;

    @Column(name = "workday_wd_num")
    private Short workdayWdNum;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "last_error_at")
    private OffsetDateTime lastErrorAt;

    @Column(name = "last_success_at")
    private OffsetDateTime lastSuccessAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public JobAlert getAlert() { return alert; }
    public void setAlert(JobAlert alert) { this.alert = alert; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getBoardToken() { return boardToken; }
    public void setBoardToken(String boardToken) { this.boardToken = boardToken; }
    public String getWorkdayTenant() { return workdayTenant; }
    public void setWorkdayTenant(String workdayTenant) { this.workdayTenant = workdayTenant; }
    public String getWorkdaySite() { return workdaySite; }
    public void setWorkdaySite(String workdaySite) { this.workdaySite = workdaySite; }
    public Short getWorkdayWdNum() { return workdayWdNum; }
    public void setWorkdayWdNum(Short workdayWdNum) { this.workdayWdNum = workdayWdNum; }
    public int getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(int displayOrder) { this.displayOrder = displayOrder; }
    public String getLastErrorMessage() { return lastErrorMessage; }
    public void setLastErrorMessage(String lastErrorMessage) { this.lastErrorMessage = lastErrorMessage; }
    public OffsetDateTime getLastErrorAt() { return lastErrorAt; }
    public void setLastErrorAt(OffsetDateTime lastErrorAt) { this.lastErrorAt = lastErrorAt; }
    public OffsetDateTime getLastSuccessAt() { return lastSuccessAt; }
    public void setLastSuccessAt(OffsetDateTime lastSuccessAt) { this.lastSuccessAt = lastSuccessAt; }
}
