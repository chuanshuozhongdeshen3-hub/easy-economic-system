package com.moon.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TaxCalcRequest {
    /**
     * 基础金额，单位分
     */
    @NotNull
    @Positive
    private Long amountCent;

    /**
     * 税率 GUID（优先）
     */
    private String taxTableGuid;

    /**
     * 税率百分比，可用于不选择税率的临时计算
     */
    private Double ratePercent;
}

