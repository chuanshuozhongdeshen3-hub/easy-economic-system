package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.BalanceSheetResponse;
import com.moon.backend.dto.CashFlowResponse;
import com.moon.backend.dto.ProfitLossResponse;
import com.moon.backend.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/pl")
    public ResponseEntity<ApiResponse<ProfitLossResponse>> profitLoss(
            @RequestParam String bookGuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        ProfitLossResponse resp = reportService.profitLoss(bookGuid, start, end);
        return ResponseEntity.ok(ApiResponse.ok("查询成功", resp));
    }

    @GetMapping("/bs")
    public ResponseEntity<ApiResponse<BalanceSheetResponse>> balanceSheet(
            @RequestParam String bookGuid,
            @RequestParam("asOf") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        BalanceSheetResponse resp = reportService.balanceSheet(bookGuid, asOf);
        return ResponseEntity.ok(ApiResponse.ok("查询成功", resp));
    }

    @GetMapping("/cf")
    public ResponseEntity<ApiResponse<CashFlowResponse>> cashFlow(
            @RequestParam String bookGuid,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end
    ) {
        CashFlowResponse resp = reportService.cashFlowNet(bookGuid, start, end);
        return ResponseEntity.ok(ApiResponse.ok("查询成功", resp));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
