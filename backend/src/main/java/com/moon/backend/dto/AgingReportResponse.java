package com.moon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgingReportResponse {
    private String ownerGuid;
    private String ownerName;
    private Long opening; // 期初余额（分）
    private Long debits;  // 本期借方
    private Long credits; // 本期贷方
    private Long closing; // 期末余额
    private List<Line> lines;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private String txGuid;
        private String num;
        private String description;
        private String accountType; // ASSET/LIABILITY
        private Long amount; // 分
        private String postDate;
    }
}
