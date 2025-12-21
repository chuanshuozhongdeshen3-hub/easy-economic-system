package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateAccountRequest {
    @NotBlank
    private String guid;

    private String name;

    private String code;

    private String description;
}
