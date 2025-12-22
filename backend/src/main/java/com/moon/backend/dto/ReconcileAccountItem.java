package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ReconcileAccountItem {
    private LocalDateTime date;
    private String memo;
    private Long amountCent;
    private String guid;
}

