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

    private String description;
}
