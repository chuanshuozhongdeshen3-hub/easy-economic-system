package com.moon.backend.controller;

import com.moon.backend.dto.ApiResponse;
import com.moon.backend.dto.ReconcileAccountItem;
import com.moon.backend.dto.ReconcileAccountResponse;
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
import java.time.LocalDateTime;
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

    @GetMapping("/account")
    public ResponseEntity<ApiResponse<ReconcileAccountResponse>> accountSplits(@RequestParam String bookGuid,
                                                                              @RequestParam String accountGuid,
                                                                              @RequestParam(required = false) LocalDate start,
                                                                              @RequestParam(required = false) LocalDate end,
                                                                              @RequestParam(defaultValue = "false") boolean includeChildren) {
        List<String> accountGuids;
        if (includeChildren) {
            accountGuids = jdbcTemplate.queryForList(
                    """
                            WITH RECURSIVE sub AS (
                                SELECT guid, parent_guid
                                  FROM accounts
                                 WHERE guid = ?
                                   AND book_guid = ?
                                UNION ALL
                                SELECT a.guid, a.parent_guid
                                  FROM accounts a
                                  JOIN sub s ON a.parent_guid = s.guid
                                 WHERE a.book_guid = ?
                            )
                            SELECT guid FROM sub
                            """,
                    String.class,
                    accountGuid,
                    bookGuid,
                    bookGuid
            );
        } else {
            accountGuids = java.util.Collections.singletonList(accountGuid);
        }

        if (accountGuids == null || accountGuids.isEmpty()) {
            throw new IllegalArgumentException("未找到匹配的科目");
        }

        String placeholders = String.join(",", java.util.Collections.nCopies(accountGuids.size(), "?"));
        StringBuilder sql = new StringBuilder("""
                SELECT s.guid,
                       COALESCE(s.memo,'') AS memo,
                       s.value_num,
                       s.value_denom,
                       t.post_date
                  FROM splits s
                  JOIN transactions t ON s.tx_guid = t.guid
                 WHERE t.book_guid = ?
                   AND s.account_guid IN (
                """);
        sql.append(placeholders).append(")");
        java.util.List<Object> args = new java.util.ArrayList<>();
        args.add(bookGuid);
        args.addAll(accountGuids);
        if (start != null) {
            sql.append(" AND t.post_date >= ? ");
            args.add(start.atStartOfDay());
        }
        if (end != null) {
            sql.append(" AND t.post_date <= ? ");
            args.add(end.plusDays(1).atStartOfDay().minusNanos(1));
        }
        sql.append(" ORDER BY t.post_date ASC");

        List<ReconcileAccountItem> items = jdbcTemplate.query(sql.toString(), (rs, i) -> {
            long num = rs.getLong("value_num");
            long denom = rs.getLong("value_denom");
            long amount = denom != 0 ? num * 100 / denom : num;
            return new ReconcileAccountItem(
                    rs.getTimestamp("post_date") != null ? rs.getTimestamp("post_date").toLocalDateTime() : LocalDateTime.now(),
                    rs.getString("memo"),
                    amount,
                    rs.getString("guid")
            );
        }, args.toArray());
        long total = items.stream().mapToLong(ReconcileAccountItem::getAmountCent).sum();
        return ResponseEntity.ok(ApiResponse.ok("查询成功", new ReconcileAccountResponse(items, total)));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ApiResponse.fail(ex.getMessage()));
    }
}
