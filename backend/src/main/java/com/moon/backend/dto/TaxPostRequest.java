package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaxPostRequest {
    @NotBlank
    private String bookGuid;

    /**
     * 基础金额，单位分
     */
    @NotNull
    @Positive
    private Long amountCent;

    @NotBlank
    private String taxTableGuid;

    /**
     * 基础科目（输入税：费用/成本类；输出税：收入类）
     */
    @NotBlank
    private String baseAccountGuid;

    /**
     * 现金科目（用于平衡，默认银行存款）
     */
    private String cashAccountGuid;

    private String description;

    private LocalDateTime postDate;
}

