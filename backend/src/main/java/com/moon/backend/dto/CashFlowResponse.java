package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CashFlowResponse {
    private BigDecimal beginBalance;
    private BigDecimal endBalance;
    private BigDecimal netChange;
}
