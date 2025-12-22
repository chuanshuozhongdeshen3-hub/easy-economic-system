package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EntryItemRequest {
    /**
     * 对应科目
     */
    @NotBlank
    private String accountGuid;

    /**
     * 金额（分）
     */
    @NotNull
    @Min(1)
    private Long amountCent;

    /**
     * 税率（百分比，可选，示例 13 表示 13%），仅用于保留，暂不计算税额
     */
    private Double taxRatePercent;

    /**
     * 金额是否含税（可选，默认不含税）
     */
    private Boolean taxIncluded;

    private String description;
}
