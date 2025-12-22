package com.moon.backend.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BankReconcileRequest {

    /**
     * 银行流水分录（split）GUID 列表
     */
    @NotEmpty
    private List<String> bankSplitGuids;

    /**
     * 业务分录 GUID 列表
     */
    @NotEmpty
    private List<String> bizSplitGuids;

    private LocalDate reconcileDate;
}
