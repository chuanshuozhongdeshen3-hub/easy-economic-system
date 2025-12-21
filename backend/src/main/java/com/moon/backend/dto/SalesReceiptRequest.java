package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SalesReceiptRequest {
    @NotBlank
    private String bookGuid;

    private String receiptNo;

    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;

    /**
     * 收款账户名称，默认银行存款
     */
    private String cashAccountName;
}
