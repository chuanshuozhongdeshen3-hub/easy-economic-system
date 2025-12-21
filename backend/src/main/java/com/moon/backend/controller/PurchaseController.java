package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.PurchaseInvoicePostRequest;
import com.moon.backend.dto.PurchasePaymentRequest;
import com.moon.backend.service.PurchaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseService purchaseService;

    @PostMapping("/invoice/post")
    public ResponseEntity<ApiResponse<Void>> postInvoice(@Valid @RequestBody PurchaseInvoicePostRequest request) {
        purchaseService.postInvoice(request);
        return ResponseEntity.ok(ApiResponse.ok("采购发票过账成功", null));
    }

    @PostMapping("/payment/post")
    public ResponseEntity<ApiResponse<Void>> postPayment(@Valid @RequestBody PurchasePaymentRequest request) {
        purchaseService.postPayment(request);
        return ResponseEntity.ok(ApiResponse.ok("采购付款过账成功", null));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
