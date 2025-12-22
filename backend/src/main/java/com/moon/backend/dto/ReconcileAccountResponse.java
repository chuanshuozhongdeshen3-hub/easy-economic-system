package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ReconcileAccountResponse {
    private List<ReconcileAccountItem> items;
    private Long totalCent;
}

