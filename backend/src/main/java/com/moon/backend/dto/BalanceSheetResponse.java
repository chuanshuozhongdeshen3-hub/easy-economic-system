package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BalanceSheetResponse {
    private List<AccountNodeResponse> assets;
    private List<AccountNodeResponse> liabilities;
    private List<AccountNodeResponse> equity;
    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
}
