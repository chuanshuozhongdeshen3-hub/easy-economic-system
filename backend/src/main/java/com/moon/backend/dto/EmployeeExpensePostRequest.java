package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EmployeeExpensePostRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String employeeGuid;

    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;

    /**
     * 借方费用科目名称，默认管理费用
     */
    private String debitAccountName;
}
