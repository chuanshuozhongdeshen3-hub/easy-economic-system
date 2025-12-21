package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 采购付款过账请求
 */
@Data
public class PurchasePaymentRequest {
    @NotBlank
    private String bookGuid;

    /**
     * 对应发票或付款单号，可为空
     */
    private String payNo;

    /**
     * 付款金额，人民币元（含税）
     */
    @NotNull
    @Min(0)
    private Long amountCent;

    /**
     * 备注
     */
    private String description;

    /**
     * 付款账户名称，未传则默认“银行存款”
     */
    private String cashAccountName;
}
