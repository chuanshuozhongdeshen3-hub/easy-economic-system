package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SalesInvoicePostRequest {
    @NotBlank
    private String bookGuid;

    private String invoiceNo;

    /**
     * 金额（分，含税）
     */
    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;
}
