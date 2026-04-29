package com.jobtracker.apps.domain.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class User extends Auditable {
    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash")
    private String passwordHash;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // getters/setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    @Column(name = "display_name")
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserTier tier = UserTier.FREE;

    @Column(name = "tier_updated_at")
    private Instant tierUpdatedAt;

    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public UserTier getTier() { return tier; }
    public void setTier(UserTier tier) { this.tier = tier; }
    public Instant getTierUpdatedAt() { return tierUpdatedAt; }
    public void setTierUpdatedAt(Instant tierUpdatedAt) { this.tierUpdatedAt = tierUpdatedAt; }
}