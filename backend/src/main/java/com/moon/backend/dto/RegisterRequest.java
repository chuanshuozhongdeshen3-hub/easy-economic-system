package com.moon.backend.dto;

import jakarta.validation.constraints.NotBlank;
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
     * 注册资本，分子 / 分母，可选；若仅提供分子则默认分母为 100。
     */
    private Long registeredCapitalNum;
    private Long registeredCapitalDenom;

    /**
     * 会计年度起始（可选）
     */
    private Integer fiscalYearStartMonth;
    private Integer fiscalYearStartDay;
}

