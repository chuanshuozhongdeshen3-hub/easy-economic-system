package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.BankReconcileRequest;
import com.moon.backend.dto.BankStatementItemRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/bank")
@RequiredArgsConstructor
public class BankController {

    private final JdbcTemplate jdbcTemplate;

    @PostMapping("/statement/import")
    public ResponseEntity<ApiResponse<String>> importStatement(@Valid @RequestBody BankStatementItemRequest request) {
        String txGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'POSTED', 'BANK_STATEMENT', ?)",
                txGuid,
                request.getBookGuid(),
                request.getRefNo(),
                request.getPostDate().atStartOfDay(),
                now,
                request.getDescription(),
                request.getRefNo()
        );

        String bankAccountGuid = jdbcTemplate.query(
                "SELECT guid FROM accounts WHERE book_guid = ? AND name = '银行存款' LIMIT 1",
                rs -> rs.next() ? rs.getString("guid") : null,
                request.getBookGuid()
        );
        if (bankAccountGuid == null) {
            return ResponseEntity.badRequest().body(ApiResponse.fail("未找到银行存款科目"));
        }

        jdbcTemplate.update(
                "INSERT INTO splits (guid, tx_guid, account_guid, value_num, value_denom, quantity_num, quantity_denom, memo, action, reconcile_state, reconcile_date, lot_guid) " +
                        "VALUES (?, ?, ?, ?, 100, ?, 100, ?, NULL, 'N', NULL, NULL)",
                UUID.randomUUID().toString(),
                txGuid,
                bankAccountGuid,
                request.getAmountCent(),
                request.getAmountCent(),
                request.getDescription()
        );

        return ResponseEntity.ok(ApiResponse.ok("导入成功", txGuid));
    }

    @PostMapping("/reconcile")
    public ResponseEntity<ApiResponse<Void>> reconcile(@Valid @RequestBody BankReconcileRequest request) {
        LocalDate date = request.getReconcileDate() != null ? request.getReconcileDate() : LocalDate.now();

        // 将银行流水分录和业务分录都标记对账
        request.getBankSplitGuids().forEach(guid -> markReconciled(guid, date));
        request.getBizSplitGuids().forEach(guid -> markReconciled(guid, date));

        return ResponseEntity.ok(ApiResponse.ok("对账成功", null));
    }

    private void markReconciled(String splitGuid, LocalDate date) {
        jdbcTemplate.update(
                "UPDATE splits SET reconcile_state = 'Y', reconcile_date = ? WHERE guid = ?",
                date,
                splitGuid
        );
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
