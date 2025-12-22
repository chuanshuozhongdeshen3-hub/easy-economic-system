package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SalesReceiptRequest {
    @NotBlank
    private String bookGuid;

    private String receiptNo;

    /**
     * 关联销售发票 GUID，用于回写结算状态
     */
    private String invoiceGuid;

    /**
     * 发票号（可选，结算描述用）
     */
    private String invoiceNo;

    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;

    /**
     * 收款账户名称，默认银行存款
     */
    private String cashAccountName;

    /**
     * 收款日期（可选），未传则当前时间
     */
    private LocalDateTime receiptDate;
}
