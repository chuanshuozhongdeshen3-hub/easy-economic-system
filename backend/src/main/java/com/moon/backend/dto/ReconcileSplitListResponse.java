package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReconcileSplitListResponse {
    private List<ReconcileSplitOption> bank;
    private List<ReconcileSplitOption> biz;
}
