package com.moon.backend.service;

import com.moon.backend.dto.PurchaseInvoicePostRequest;
import com.moon.backend.dto.PurchasePaymentRequest;
import com.moon.backend.entity.Account;
import com.moon.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 采购发票过账：借 费用/存货，贷 应付账款。
     */
    @Transactional
    public void postInvoice(PurchaseInvoicePostRequest request) {
        String bookGuid = request.getBookGuid();
        long cents = request.getAmountCent();
        if (cents <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }

        Account debit = resolveDebitAccount(bookGuid, request.getDebitAccountName());
        Account ap = resolveByName(bookGuid, "应付账款")
                .orElseThrow(() -> new IllegalStateException("未找到“应付账款”科目"));

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
                coalesce(request.getDescription(), "采购发票过账"),
                "PURCHASE_INVOICE",
                request.getInvoiceNo()
        );

        // 借：费用/存货
        insertSplit(txGuid, debit.getGuid(), cents, request.getDescription());
        // 贷：应付账款
        insertSplit(txGuid, ap.getGuid(), -cents, request.getDescription());
    }

    /**
     * 采购付款过账：借 应付账款，贷 银行存款。
     */
    @Transactional
    public void postPayment(PurchasePaymentRequest request) {
        String bookGuid = request.getBookGuid();
        long cents = request.getAmountCent();
        if (cents <= 0) {
            throw new IllegalArgumentException("金额必须大于 0");
        }

        Account ap = resolveByName(bookGuid, "应付账款")
                .orElseThrow(() -> new IllegalStateException("未找到“应付账款”科目"));
        Account cash = resolveCashAccount(bookGuid, request.getCashAccountName());

        LocalDateTime now = LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();

        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 'POSTED', ?, ?)",
                txGuid,
                bookGuid,
                request.getPayNo(),
                now,
                now,
                coalesce(request.getDescription(), "采购付款过账"),
                "PURCHASE_PAYMENT",
                request.getPayNo()
        );

        // 借：应付账款
        insertSplit(txGuid, ap.getGuid(), cents, request.getDescription());
        // 贷：银行存款
        insertSplit(txGuid, cash.getGuid(), -cents, request.getDescription());
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

    private Account resolveDebitAccount(String bookGuid, String preferName) {
        if (preferName != null && !preferName.isBlank()) {
            return resolveByName(bookGuid, preferName)
                    .orElseThrow(() -> new IllegalArgumentException("未找到科目：" + preferName));
        }
        // 优先存货，其次管理费用
        return resolveByName(bookGuid, "存货")
                .or(() -> resolveByName(bookGuid, "主营业务成本"))
                .or(() -> resolveByName(bookGuid, "管理费用"))
                .orElseThrow(() -> new IllegalStateException("未找到可用的费用/存货科目"));
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
