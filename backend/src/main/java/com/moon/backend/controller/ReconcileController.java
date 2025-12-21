package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.ReconcileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/reconcile")
@RequiredArgsConstructor
public class ReconcileController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/splits")
    public ResponseEntity<ApiResponse<Void>> reconcileSplits(@RequestBody ReconcileRequest request) {
        LocalDate date = request.getReconcileDate() != null ? request.getReconcileDate() : LocalDate.now();
        for (String guid : request.getSplitGuids()) {
            jdbcTemplate.update(
                    "UPDATE splits SET reconcile_state = 'Y', reconcile_date = ? WHERE guid = ?",
                    date,
                    guid
            );
        }
        return ResponseEntity.ok(ApiResponse.ok("对账成功", null));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
