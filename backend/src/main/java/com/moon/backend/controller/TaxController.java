package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.TaxCalcRequest;
import com.moon.backend.dto.TaxCalcResponse;
import com.moon.backend.dto.TaxPostRequest;
import com.moon.backend.dto.TaxRateRequest;
import com.moon.backend.dto.TaxRateResponse;
import com.moon.backend.service.TaxService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tax")
@RequiredArgsConstructor
public class TaxController {

    private final TaxService taxService;

    @GetMapping("/rates")
    public ResponseEntity<ApiResponse<List<TaxRateResponse>>> list(@RequestParam String bookGuid) {
        return ResponseEntity.ok(ApiResponse.ok("查询成功", taxService.listRates(bookGuid)));
    }

    @PostMapping("/rates")
    public ResponseEntity<ApiResponse<String>> create(@Valid @RequestBody TaxRateRequest request) {
        String guid = taxService.createRate(request);
        return ResponseEntity.ok(ApiResponse.ok("税率创建成功", guid));
    }

    @PostMapping("/calc")
    public ResponseEntity<ApiResponse<TaxCalcResponse>> calc(@Valid @RequestBody TaxCalcRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("计算成功", taxService.calculate(request)));
    }

    @PostMapping("/post")
    public ResponseEntity<ApiResponse<String>> post(@Valid @RequestBody TaxPostRequest request) {
        String txGuid = taxService.postTax(request);
        return ResponseEntity.ok(ApiResponse.ok("过账成功", txGuid));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}

