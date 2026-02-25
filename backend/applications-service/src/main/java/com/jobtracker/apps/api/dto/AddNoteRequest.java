package com.jobtracker.apps.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AddNoteRequest(@NotBlank String body) {}