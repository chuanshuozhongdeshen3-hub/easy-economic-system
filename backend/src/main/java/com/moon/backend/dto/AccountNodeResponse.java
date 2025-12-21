package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountNodeResponse {
    private String guid;
    private String name;
    private String code;
    private String accountType;
    private String description;
    /**
     * 金额（包含本级 + 下级汇总）
     */
    private BigDecimal balance = BigDecimal.ZERO;
    private List<AccountNodeResponse> children = new ArrayList<>();
}
