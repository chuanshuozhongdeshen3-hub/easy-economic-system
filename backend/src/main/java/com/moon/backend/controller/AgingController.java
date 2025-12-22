package com.moon.backend.controller;

import com.moon.backend.dto.AgingReportResponse;
import com.moon.backend.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/aging")
@RequiredArgsConstructor
public class AgingController {

    private final JdbcTemplate jdbcTemplate;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AgingReportResponse>>> report(
            @RequestParam String bookGuid,
            @RequestParam(defaultValue = "CUSTOMER") String ownerType, // CUSTOMER or VENDOR
            @RequestParam(required = false) String ownerGuid
    ) {
        List<AgingReportResponse> list = new ArrayList<>();

        jdbcTemplate.query(
                """
                SELECT o.guid AS owner_guid, o.name AS owner_name
                  FROM owner o
                 WHERE o.book_guid = ?
                   AND o.owner_type = ?
                   AND (? IS NULL OR o.guid = ?)
                """,
                ps -> {
                    ps.setString(1, bookGuid);
                    ps.setString(2, ownerType);
                    ps.setString(3, ownerGuid);
                    ps.setString(4, ownerGuid);
                },
                rs -> {
                    String og = rs.getString("owner_guid");
                    AgingReportResponse r = new AgingReportResponse();
                    r.setOwnerGuid(og);
                    r.setOwnerName(rs.getString("owner_name"));
                    fillBalances(r, bookGuid, og, ownerType);
                    list.add(r);
                }
        );

        return ResponseEntity.ok(ApiResponse.ok("查询成功", list));
    }

    private void fillBalances(AgingReportResponse r, String bookGuid, String ownerGuid, String ownerType) {
        // 期初余额（假设无期初表，先用0）
        long opening = 0L;

        // 本期发生（借贷按科目类型方向，ASSET=应收，LIABILITY=应付）
        // 此处简化：查询应收/应付科目的 splits 汇总
        Long debits = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(CASE WHEN s.value_num > 0 THEN s.value_num ELSE 0 END),0)
                  FROM splits s
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE a.book_guid = ?
                   AND ((? = 'CUSTOMER' AND a.account_type = 'ASSET') OR (? = 'VENDOR' AND a.account_type = 'LIABILITY'))
                   AND s.tx_guid IN (
                        SELECT t.guid FROM transactions t
                        JOIN invoices i ON t.source_guid = i.guid OR t.num = i.id
                        WHERE i.owner_guid = ?
                   )
                """,
                Long.class,
                bookGuid,
                ownerType,
                ownerType,
                ownerGuid
        );
        Long credits = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(CASE WHEN s.value_num < 0 THEN ABS(s.value_num) ELSE 0 END),0)
                  FROM splits s
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE a.book_guid = ?
                   AND ((? = 'CUSTOMER' AND a.account_type = 'ASSET') OR (? = 'VENDOR' AND a.account_type = 'LIABILITY'))
                   AND s.tx_guid IN (
                        SELECT t.guid FROM transactions t
                        JOIN invoices i ON t.source_guid = i.guid OR t.num = i.id
                        WHERE i.owner_guid = ?
                   )
                """,
                Long.class,
                bookGuid,
                ownerType,
                ownerType,
                ownerGuid
        );

        long d = debits == null ? 0 : debits;
        long c = credits == null ? 0 : credits;
        long closing = opening + d - c;
        r.setOpening(opening);
        r.setDebits(d);
        r.setCredits(c);
        r.setClosing(closing);

        // 明细行（简化：列出相关交易）
        List<AgingReportResponse.Line> lines = new ArrayList<>();
        jdbcTemplate.query(
                """
                SELECT t.guid, t.num, t.description, t.post_date, a.account_type, s.value_num, s.value_denom
                  FROM transactions t
                  JOIN splits s ON t.guid = s.tx_guid
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE a.book_guid = ?
                   AND ((? = 'CUSTOMER' AND a.account_type = 'ASSET') OR (? = 'VENDOR' AND a.account_type = 'LIABILITY'))
                   AND t.source_guid IN (SELECT guid FROM invoices WHERE owner_guid = ?)
                """,
                ps -> {
                    ps.setString(1, bookGuid);
                    ps.setString(2, ownerType);
                    ps.setString(3, ownerType);
                    ps.setString(4, ownerGuid);
                },
                rs -> {
                    long val = rs.getLong("value_num");
                    long denom = rs.getLong("value_denom");
                    if (denom != 0) val = val / denom * 100; // 简化处理
                    lines.add(new AgingReportResponse.Line(
                            rs.getString("guid"),
                            rs.getString("num"),
                            rs.getString("description"),
                            rs.getString("account_type"),
                            val,
                            rs.getString("post_date")
                    ));
                }
        );
        r.setLines(lines);
    }
}
