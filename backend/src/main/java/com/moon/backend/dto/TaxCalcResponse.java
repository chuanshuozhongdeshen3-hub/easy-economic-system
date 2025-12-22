package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaxCalcResponse {
    private Long baseCent;
    private Long taxCent;
    private Long totalCent;
    private Double ratePercent;
    private String rateName;
}

