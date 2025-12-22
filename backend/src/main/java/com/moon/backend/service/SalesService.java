package com.moon.backend.service;

import com.moon.backend.dto.SalesInvoicePostRequest;
import com.moon.backend.dto.SalesReceiptRequest;
import com.moon.backend.entity.Account;
import com.moon.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SalesService {

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 销售发票过账：借 应收账款，贷 主营业务收入。
     */
    @Transactional
    public void postInvoice(SalesInvoicePostRequest request) {
        long cents = request.getAmountCent();
        if (cents <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        String bookGuid = request.getBookGuid();
        Account ar = resolveByName(bookGuid, "应收账款")
                .orElseThrow(() -> new IllegalStateException("未找到“应收账款”科目"));
        Account revenue = resolveByName(bookGuid, "主营业务收入")
                .orElseThrow(() -> new IllegalStateException("未找到“主营业务收入”科目"));

        LocalDateTime now = LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'POSTED', ?, ?)",
                txGuid,
                bookGuid,
                request.getInvoiceNo(),
                now,
                now,
                coalesce(request.getDescription(), "销售发票过账"),
                "SALES_INVOICE",
                request.getInvoiceGuid() != null ? request.getInvoiceGuid() : request.getInvoiceNo()
        );

        insertSplit(txGuid, ar.getGuid(), cents, request.getDescription());
        insertSplit(txGuid, revenue.getGuid(), -cents, request.getDescription());

        if (request.getInvoiceGuid() != null && !request.getInvoiceGuid().isBlank()) {
            jdbcTemplate.update(
                    "UPDATE invoices SET status = 'POSTED', post_txn_guid = ? WHERE guid = ?",
                    txGuid,
                    request.getInvoiceGuid()
            );
            updateInvoiceSettlement(bookGuid, request.getInvoiceGuid(), "ASSET");
        }
    }

    /**
     * 收款过账：借 银行存款，贷 应收账款。
     */
    @Transactional
    public void postReceipt(SalesReceiptRequest request) {
        long cents = request.getAmountCent();
        if (cents <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }
        String bookGuid = request.getBookGuid();
        Account ar = resolveByName(bookGuid, "应收账款")
                .orElseThrow(() -> new IllegalStateException("未找到“应收账款”科目"));
        Account cash = resolveCashAccount(bookGuid, request.getCashAccountName());

        LocalDateTime now = LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'POSTED', ?, ?)",
                txGuid,
                bookGuid,
                request.getReceiptNo(),
                now,
                now,
                coalesce(request.getDescription(), "销售收款过账"),
                "SALES_RECEIPT",
                request.getInvoiceGuid() != null ? request.getInvoiceGuid() : request.getReceiptNo()
        );

        insertSplit(txGuid, cash.getGuid(), cents, request.getDescription());
        insertSplit(txGuid, ar.getGuid(), -cents, request.getDescription());

        if (hasText(request.getInvoiceGuid())) {
            jdbcTemplate.update(
                    "UPDATE invoices SET status = 'APPROVED' WHERE guid = ?",
                    request.getInvoiceGuid()
            );
            jdbcTemplate.update(
                    "UPDATE invoices SET notes = CONCAT(COALESCE(notes,''), ?) WHERE guid = ?",
                    " | 已收款金额(分):" + cents,
                    request.getInvoiceGuid()
            );
            updateInvoiceSettlement(bookGuid, request.getInvoiceGuid(), "ASSET");
        }
    }

    private void insertSplit(String txGuid, String accountGuid, long cents, String memo) {
        LocalDateTime now = LocalDateTime.now();
        jdbcTemplate.update(
                "INSERT INTO splits (guid, tx_guid, account_guid, value_num, value_denom, quantity_num, quantity_denom, memo, action, reconcile_state, reconcile_date, lot_guid) " +
                        "VALUES (?, ?, ?, ?, 100, ?, 100, ?, NULL, 'N', NULL, NULL)",
                UUID.randomUUID().toString(),
                txGuid,
                accountGuid,
                cents,
                cents,
                memo
        );
    }

    private Account resolveCashAccount(String bookGuid, String preferName) {
        if (preferName != null && !preferName.isBlank()) {
            return resolveByName(bookGuid, preferName)
                    .orElseThrow(() -> new IllegalArgumentException("未找到科目：" + preferName));
        }
        return resolveByName(bookGuid, "银行存款")
                .orElseThrow(() -> new IllegalStateException("未找到“银行存款”科目"));
    }

    private Optional<Account> resolveByName(String bookGuid, String name) {
        return accountRepository.findFirstByBookGuidAndName(bookGuid, name);
    }

    private String coalesce(String v, String def) {
        return (v == null || v.isBlank()) ? def : v;
    }

    private boolean hasText(String v) {
        return v != null && !v.isBlank();
    }

    private void updateInvoiceSettlement(String bookGuid, String invoiceGuid, String accountType) {
        Long total = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(price_num / NULLIF(price_denom,0)),0) FROM entries WHERE invoice_guid = ? AND book_guid = ?",
                Long.class,
                invoiceGuid,
                bookGuid
        );
        Long settled = jdbcTemplate.queryForObject(
                """
                SELECT COALESCE(SUM(ABS(s.value_num / NULLIF(s.value_denom,0))),0)
                  FROM splits s
                  JOIN transactions t ON s.tx_guid = t.guid
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE t.source_guid = ? AND t.book_guid = ? AND a.account_type = ?
                """,
                Long.class,
                invoiceGuid,
                bookGuid,
                accountType
        );
        if (total == null || total == 0) {
            return;
        }
        String newStatus = (settled != null && settled >= total) ? "APPROVED" : "POSTED";
        jdbcTemplate.update(
                "UPDATE invoices SET status = ? WHERE guid = ?",
                newStatus,
                invoiceGuid
        );
        jdbcTemplate.update(
                "UPDATE invoices SET notes = CONCAT(COALESCE(notes,''), ?) WHERE guid = ?",
                " | 已结算金额(分):" + (settled == null ? 0 : settled),
                invoiceGuid
        );
    }
}
