package com.jobtracker.apps.api.dto;

public record AuthResponse(String token, String email, String userId, String displayName, String tier) {}
