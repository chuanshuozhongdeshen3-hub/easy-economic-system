package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 快速平衡性校验：资产-负债=所有者权益，资产+费用=负债+所有者权益+收入
 */
@RestController
@RequestMapping("/api/balance")
@RequiredArgsConstructor
public class BalanceController {

    private final AccountService accountService;

    @GetMapping("/check")
    public ResponseEntity<ApiResponse<String>> check(@RequestParam String bookGuid) {
        var sums = accountService.sumByType(bookGuid);
        BigDecimal asset = sums.getOrDefault("ASSET", BigDecimal.ZERO);
        BigDecimal liability = sums.getOrDefault("LIABILITY", BigDecimal.ZERO);
        BigDecimal equity = sums.getOrDefault("EQUITY", BigDecimal.ZERO);
        BigDecimal income = sums.getOrDefault("INCOME", BigDecimal.ZERO);
        BigDecimal expense = sums.getOrDefault("EXPENSE", BigDecimal.ZERO);

        BigDecimal staticDiff = asset.subtract(liability.add(equity)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal dynamicDiff = asset.add(expense).subtract(liability.add(equity).add(income)).setScale(2, RoundingMode.HALF_UP);

        String msg = "静态差额：" + staticDiff + "，动态差额：" + dynamicDiff;
        boolean ok = staticDiff.compareTo(BigDecimal.ZERO) == 0 && dynamicDiff.compareTo(BigDecimal.ZERO) == 0;
        return ResponseEntity.ok(ok ? ApiResponse.ok("平衡", msg) : ApiResponse.fail(msg));
    }
}
