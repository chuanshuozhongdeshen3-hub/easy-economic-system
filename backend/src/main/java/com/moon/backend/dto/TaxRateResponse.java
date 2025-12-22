package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TaxRateResponse {
    private String guid;
    private String name;
    private Double ratePercent;
    private String direction;
    private String payableAccountGuid;
    private String description;
    private Boolean active;
}

