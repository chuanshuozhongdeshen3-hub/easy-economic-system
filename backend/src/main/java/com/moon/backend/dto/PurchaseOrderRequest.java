package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PurchaseOrderRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String vendorGuid;

    /**
     * 订单编号
     */
    private String orderId;

    /**
     * 项目/作业 GUID（可选）
     */
    private String jobGuid;

    private String notes;
}
