package com.jobtracker.ai.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParseApplicationRequest(
        @NotBlank @Size(max = 4000) String description
) {}
