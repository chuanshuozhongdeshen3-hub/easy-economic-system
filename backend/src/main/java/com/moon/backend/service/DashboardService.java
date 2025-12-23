package com.moon.backend.service;

import com.moon.backend.dto.AccountNodeResponse;
import com.moon.backend.dto.DashboardSummary;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private final AccountService accountService;

    public DashboardSummary buildSummary(String bookGuid) {
        BigDecimal cash = queryCash(bookGuid);
        BigDecimal arOutstanding = safeAbs(queryBalanceByName(bookGuid, "应收账款"));
        BigDecimal apOutstanding = safeAbs(queryBalanceByName(bookGuid, "应付账款"));

        BigDecimal billedAr = safeAbs(queryInvoiceSide(bookGuid, "SALES_INVOICE", "应收账款"));
        BigDecimal billedAp = safeAbs(queryInvoiceSide(bookGuid, "PURCHASE_INVOICE", "应付账款"));

        BigDecimal received = billedAr.max(BigDecimal.ZERO).subtract(arOutstanding);
        if (received.compareTo(BigDecimal.ZERO) < 0) received = BigDecimal.ZERO;
        BigDecimal paid = billedAp.max(BigDecimal.ZERO).subtract(apOutstanding);
        if (paid.compareTo(BigDecimal.ZERO) < 0) paid = BigDecimal.ZERO;

        BigDecimal recvProgress = billedAr.compareTo(BigDecimal.ZERO) > 0
                ? received.multiply(BigDecimal.valueOf(100)).divide(billedAr, 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);
        BigDecimal payProgress = billedAp.compareTo(BigDecimal.ZERO) > 0
                ? paid.multiply(BigDecimal.valueOf(100)).divide(billedAp, 2, RoundingMode.HALF_UP)
                : BigDecimal.valueOf(100);

        return new DashboardSummary(
                cash.setScale(2, RoundingMode.HALF_UP),
                arOutstanding.setScale(2, RoundingMode.HALF_UP),
                apOutstanding.setScale(2, RoundingMode.HALF_UP),
                received.setScale(2, RoundingMode.HALF_UP),
                paid.setScale(2, RoundingMode.HALF_UP),
                recvProgress,
                payProgress,
                countBySource(bookGuid, "SALES_INVOICE"),
                countBySource(bookGuid, "SALES_RECEIPT"),
                countBySource(bookGuid, "PURCHASE_INVOICE"),
                countBySource(bookGuid, "PURCHASE_PAYMENT"),
                countPendingInvoices(bookGuid, "SALES"),
                countPendingInvoices(bookGuid, "PURCHASE"),
                safeAbs(queryBalanceByName(bookGuid, "应交税费")).setScale(2, RoundingMode.HALF_UP)
        );
    }

    private BigDecimal queryCash(String bookGuid) {
        // 资金池：仅现金+银行存款两类，包含注册资本的初始挂账
        List<AccountNodeResponse> tree = accountService.getAccountTree(bookGuid);
        Deque<AccountNodeResponse> stack = new ArrayDeque<>(tree);
        BigDecimal sum = BigDecimal.ZERO;
        while (!stack.isEmpty()) {
            AccountNodeResponse n = stack.pop();
            if ("ASSET".equalsIgnoreCase(n.getAccountType())
                    && ("现金".equals(n.getName()) || "银行存款".equals(n.getName()))) {
                sum = sum.add(n.getBalance() == null ? BigDecimal.ZERO : n.getBalance());
            }
            if (n.getChildren() != null && !n.getChildren().isEmpty()) {
                stack.addAll(n.getChildren());
            }
        }
        return sum;
    }

    private BigDecimal queryBalanceByName(String bookGuid, String name) {
        BigDecimal sum = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(s.value_num / NULLIF(s.value_denom,0)),0)
                          FROM splits s
                          JOIN transactions t ON s.tx_guid = t.guid
                          JOIN accounts a ON s.account_guid = a.guid
                         WHERE t.book_guid = ? AND a.name = ?
                        """,
                BigDecimal.class,
                bookGuid,
                name
        );
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private BigDecimal queryInvoiceSide(String bookGuid, String sourceType, String accountName) {
        BigDecimal sum = jdbcTemplate.queryForObject(
                """
                        SELECT COALESCE(SUM(s.value_num / NULLIF(s.value_denom,0)),0)
                          FROM splits s
                          JOIN transactions t ON s.tx_guid = t.guid
                          JOIN accounts a ON s.account_guid = a.guid
                         WHERE t.book_guid = ?
                           AND t.source_type = ?
                           AND a.name = ?
                        """,
                BigDecimal.class,
                bookGuid,
                sourceType,
                accountName
        );
        return sum == null ? BigDecimal.ZERO : sum;
    }

    private BigDecimal safeAbs(BigDecimal v) {
        if (v == null) return BigDecimal.ZERO;
        return v.abs();
    }

    private Long countBySource(String bookGuid, String sourceType) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM transactions WHERE book_guid = ? AND source_type = ?",
                Long.class,
                bookGuid,
                sourceType
        );
        return cnt == null ? 0L : cnt;
    }

    private Long countPendingInvoices(String bookGuid, String invoiceType) {
        Long cnt = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM invoices WHERE book_guid = ? AND invoice_type = ? AND status <> 'APPROVED'",
                Long.class,
                bookGuid,
                invoiceType
        );
        return cnt == null ? 0L : cnt;
    }
}
