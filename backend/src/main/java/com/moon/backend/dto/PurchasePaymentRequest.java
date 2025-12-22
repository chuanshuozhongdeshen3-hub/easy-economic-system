package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

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
     * 关联的采购发票 GUID，可选，用于回写状态/结算
     */
    private String invoiceGuid;

    /**
     * 本次支付关联的发票号（用于结算展示，可选）
     */
    private String invoiceNo;

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

    /**
     * 关联的采购订单（已过账）GUID，可选
     */
    private String orderGuid;

    /**
     * 支付日期（可选），未传则使用当前时间
     */
    private LocalDateTime payDate;
}
