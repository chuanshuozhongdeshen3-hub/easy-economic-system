package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.ReconcileRequest;
import com.moon.backend.dto.ReconcileSplitListResponse;
import com.moon.backend.dto.ReconcileSplitOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

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

    @GetMapping("/splits")
    public ResponseEntity<ApiResponse<ReconcileSplitListResponse>> listSplits(@RequestParam String bookGuid) {
        List<ReconcileSplitOption> bank = jdbcTemplate.query(
                """
                        SELECT s.guid, COALESCE(s.memo, '') AS memo, s.value_num, s.value_denom
                          FROM splits s
                          JOIN transactions t ON s.tx_guid = t.guid
                          JOIN accounts a ON s.account_guid = a.guid
                         WHERE a.book_guid = ?
                           AND s.reconcile_state <> 'Y'
                           AND (t.source_type = 'BANK_STATEMENT' OR a.name = '银行存款')
                         ORDER BY t.post_date DESC
                        """,
                (rs, rowNum) -> {
                    long num = rs.getLong("value_num");
                    long denom = rs.getLong("value_denom");
                    long amount = denom != 0 ? num / denom * 100 : num;
                    return new ReconcileSplitOption(
                            rs.getString("guid"),
                            rs.getString("memo"),
                            amount
                    );
                },
                bookGuid
        );

        List<ReconcileSplitOption> biz = jdbcTemplate.query(
                """
                        SELECT s.guid, COALESCE(s.memo, '') AS memo, s.value_num, s.value_denom
                          FROM splits s
                          JOIN transactions t ON s.tx_guid = t.guid
                          JOIN accounts a ON s.account_guid = a.guid
                         WHERE a.book_guid = ?
                           AND s.reconcile_state <> 'Y'
                           AND NOT (t.source_type = 'BANK_STATEMENT' OR a.name = '银行存款')
                         ORDER BY t.post_date DESC
                        """,
                (rs, rowNum) -> {
                    long num = rs.getLong("value_num");
                    long denom = rs.getLong("value_denom");
                    long amount = denom != 0 ? num / denom * 100 : num;
                    return new ReconcileSplitOption(
                            rs.getString("guid"),
                            rs.getString("memo"),
                            amount
                    );
                },
                bookGuid
        );

        return ResponseEntity.ok(ApiResponse.ok("查询成功", new ReconcileSplitListResponse(bank, biz)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
