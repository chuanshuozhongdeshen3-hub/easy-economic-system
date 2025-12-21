package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

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
}
