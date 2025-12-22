package com.moon.backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class BankStatementItemRequest {
    @NotBlank
    private String bookGuid;

    /**
     * 流水号/引用
     */
    private String refNo;

    /**
     * 交易日期
     */
    @NotNull
    private LocalDate postDate;

    /**
     * 金额（分），正为收入，负为支出
     */
    @NotNull
    @Min(1)
    private Long amountCent;

    private String description;
}
