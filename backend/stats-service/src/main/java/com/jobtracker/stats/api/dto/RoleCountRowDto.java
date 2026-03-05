package com.jobtracker.stats.api.dto;

public record RoleCountRowDto(
        String label,
        int periodNum,
        long engineerDev,
        long manager,
        long architect,
        long other
) {}
