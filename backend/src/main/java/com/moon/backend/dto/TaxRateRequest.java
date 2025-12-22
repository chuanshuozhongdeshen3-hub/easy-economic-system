package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class TaxRateRequest {
    @NotBlank
    private String bookGuid;

    @NotBlank
    private String name;

    /**
     * 税率，百分比形式，如 13 表示 13%
     */
    @NotNull
    @Positive
    private Double ratePercent;

    /**
     * INPUT / OUTPUT
     */
    @NotBlank
    private String direction;

    /**
     * 税额挂账科目（通常为负债类应交税费）
     */
    @NotBlank
    private String payableAccountGuid;

    private String description;
}

