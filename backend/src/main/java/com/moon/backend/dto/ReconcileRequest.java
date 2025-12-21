package com.moon.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ReconcileRequest {
    @NotEmpty
    private List<String> splitGuids;

    /**
     * 对账日期，可选；未传则使用当前日期
     */
    private LocalDate reconcileDate;
}
