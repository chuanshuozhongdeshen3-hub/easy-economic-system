package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SalesInvoiceCreateRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String customerGuid;

    /**
     * 发票编号
     */
    private String invoiceId;

    /**
     * 备注
     */
    private String notes;
}
