package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class NameStatusResponse {
    private String guid;
    private String name;
    private String status;
    private String note;
    private LocalDateTime date;
    private BigDecimal amount;
}
