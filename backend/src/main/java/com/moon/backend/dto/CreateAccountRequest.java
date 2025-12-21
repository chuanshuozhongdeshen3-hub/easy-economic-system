package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateAccountRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String parentGuid;

    @NotBlank
    private String name;

    private String code;

    @NotBlank
    private String accountType;

    private String description;
}
