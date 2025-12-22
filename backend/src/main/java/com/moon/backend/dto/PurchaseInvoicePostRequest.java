package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 采购发票过账请求
 */
@Data
public class PurchaseInvoicePostRequest {
    @NotBlank
    private String bookGuid;

    /**
     * 发票或单据编号，允许为空
     */
    private String invoiceNo;

    /**
     * 关联的采购发票 GUID，可选，用于回写状态
     */
    private String invoiceGuid;

    /**
     * 关联的采购订单 GUID，可选，仅记录来源
     */
    private String orderGuid;

    /**
     * 含税金额，人民币元
     */
    @NotNull
    @Min(0)
    private Long amountCent;

    /**
     * 备注
     */
    private String description;

    /**
     * 费用/存货科目名称，未传则优先使用“存货”，否则“管理费用”
     */
    private String debitAccountName;

    /**
     * 应付科目名称（可选，默认应付账款）
     */
    private String payableAccountName;

    /**
     * 入账时间（可选），未传则用当前时间
     */
    private LocalDateTime postDate;
}
