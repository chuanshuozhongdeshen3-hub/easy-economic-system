package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileSplitOption {
    private String guid;
    private String memo;
    /**
     * 金额，单位：分（保留 value_num，前端自行换算）
     */
    private long amount;
}
