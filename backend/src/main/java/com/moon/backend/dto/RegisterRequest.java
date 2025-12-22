package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank
    @Size(min = 6, max = 100)
    private String password;

    /**
     * 账本名称（企业名称）
     */
    @NotBlank
    private String bookName;

    /**
     * 企业规模（可选）
     */
    private String bookSize;

    /**
     * 注册资本（元），必填。
     */
    @NotNull
    @Min(1)
    private Long registeredCapitalNum;

    /**
     * 注册资本分母（可选）。默认 1，允许历史值传入。
     */
    private Long registeredCapitalDenom;

    /**
     * 会计年度起始（可选）
     */
    private Integer fiscalYearStartMonth;
    private Integer fiscalYearStartDay;
}

