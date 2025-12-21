package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CustomerRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String name;

    private String id;
    private String notes;
    private String taxId;
    private String email;
    private String phone;
    private String addr;
}
