package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.SalesInvoicePostRequest;
import com.moon.backend.dto.SalesReceiptRequest;
import com.moon.backend.service.SalesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SalesController {

    private final SalesService salesService;

    @PostMapping("/invoice/post")
    public ResponseEntity<ApiResponse<Void>> postInvoice(@Valid @RequestBody SalesInvoicePostRequest request) {
        salesService.postInvoice(request);
        return ResponseEntity.ok(ApiResponse.ok("销售发票过账成功", null));
    }

    @PostMapping("/receipt/post")
    public ResponseEntity<ApiResponse<Void>> postReceipt(@Valid @RequestBody SalesReceiptRequest request) {
        salesService.postReceipt(request);
        return ResponseEntity.ok(ApiResponse.ok("收款过账成功", null));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
