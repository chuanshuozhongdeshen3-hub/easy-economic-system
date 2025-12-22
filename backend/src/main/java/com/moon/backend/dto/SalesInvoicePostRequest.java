package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesInvoicePostRequest {
    @NotBlank
    private String bookGuid;

    private String invoiceNo;

    /**
     * 关联销售发票 GUID，用于回写状态
     */
    private String invoiceGuid;

    /**
     * 金额（分，含税）
     */
    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;

    /**
     * 应收科目名称（可选，默认应收账款）
     */
    private String receivableAccountName;

    /**
     * 入账时间（可选），未传则当前时间
     */
    private LocalDateTime postDate;
}
