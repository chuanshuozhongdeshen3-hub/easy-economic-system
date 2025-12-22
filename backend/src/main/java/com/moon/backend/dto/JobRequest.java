package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class JobRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String name;

    private String description;
}

