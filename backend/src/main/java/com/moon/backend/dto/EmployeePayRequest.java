package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmployeePayRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String employeeGuid;

    /**
     * 关联的报销/差旅（已过账）交易 GUID，可选
     */
    private String expenseGuid;

    @NotNull
    @Min(1)
    private Long amountCent;

    private String cashAccountName;
    private String description;

    /**
     * 支付日期（可选），未传则当前时间
     */
    private LocalDateTime payDate;
}
