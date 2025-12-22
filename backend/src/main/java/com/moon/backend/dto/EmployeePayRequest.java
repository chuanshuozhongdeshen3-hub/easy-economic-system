package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeePayRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String employeeGuid;

    @NotNull
    @Min(1)
    private Long amountCent;

    private String cashAccountName;
    private String description;
}
