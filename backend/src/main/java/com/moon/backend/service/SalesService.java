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
                request.getInvoiceNo()
        );

        insertSplit(txGuid, ar.getGuid(), cents, request.getDescription());
        insertSplit(txGuid, revenue.getGuid(), -cents, request.getDescription());
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
                request.getReceiptNo()
        );

        insertSplit(txGuid, cash.getGuid(), cents, request.getDescription());
        insertSplit(txGuid, ar.getGuid(), -cents, request.getDescription());
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
}
