package com.moon.backend.service;

import com.moon.backend.dto.PurchaseInvoicePostRequest;
import com.moon.backend.dto.PurchasePaymentRequest;
import com.moon.backend.entity.Account;
import com.moon.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PurchaseService {

    private final AccountRepository accountRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 采购发票过账：按 entries 汇总（数量/折扣/含税），借费用/库存+进项税，贷应付。
     */
    @Transactional
    public void postInvoice(PurchaseInvoicePostRequest request) {
        String bookGuid = request.getBookGuid();
        if (!hasText(request.getInvoiceGuid())) {
            throw new IllegalArgumentException("采购发票 GUID 不能为空，需按明细过账");
        }

        Account ap = resolveByName(bookGuid, "应付账款")
                .orElseThrow(() -> new IllegalStateException("未找到“应付账款”科目"));

        InvoiceCalc calc = loadInvoiceCalc(bookGuid, request.getInvoiceGuid());
        if (calc.totalCents <= 0) {
            throw new IllegalStateException("发票行金额合计必须大于 0");
        }

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
                coalesce(request.getInvoiceGuid(), request.getOrderGuid())
        );

        // 借：费用/库存等业务科目
        for (Map.Entry<String, Long> entry : calc.baseByAccount.entrySet()) {
            insertSplit(txGuid, entry.getKey(), entry.getValue(), request.getDescription());
        }
        // 借：进项税额
        for (Map.Entry<String, Long> entry : calc.taxByAccount.entrySet()) {
            insertSplit(txGuid, entry.getKey(), entry.getValue(), "进项税额");
        }
        // 贷：应付账款 = 含税总额
        insertSplit(txGuid, ap.getGuid(), -calc.totalCents, request.getDescription());

        if (hasText(request.getInvoiceGuid())) {
            jdbcTemplate.update(
                    "UPDATE invoices SET status = 'POSTED', post_txn_guid = ? WHERE guid = ?",
                    txGuid,
                    request.getInvoiceGuid()
            );
            jdbcTemplate.update(
                    "UPDATE invoices SET notes = CONCAT(COALESCE(notes,''), ?) WHERE guid = ?",
                    " | 已过账金额分:" + calc.totalCents,
                    request.getInvoiceGuid()
            );
            updateInvoiceSettlement(bookGuid, request.getInvoiceGuid(), "LIABILITY", calc.totalCents);
        }
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
                coalesce(request.getInvoiceGuid(), request.getPayNo())
        );

        insertSplit(txGuid, ap.getGuid(), cents, request.getDescription());
        insertSplit(txGuid, cash.getGuid(), -cents, request.getDescription());

        if (hasText(request.getInvoiceGuid())) {
            jdbcTemplate.update(
                    "UPDATE invoices SET status = 'APPROVED' WHERE guid = ?",
                    request.getInvoiceGuid()
            );
            jdbcTemplate.update(
                    "UPDATE invoices SET notes = CONCAT(COALESCE(notes,''), ?) WHERE guid = ?",
                    " | 已支付金额分:" + cents,
                    request.getInvoiceGuid()
            );
            updateInvoiceSettlement(bookGuid, request.getInvoiceGuid(), "LIABILITY", null);
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

    /**
     * 根据 entries 和已关联交易的 splits 计算结算金额并更新状态。
     */
    private void updateInvoiceSettlement(String bookGuid, String invoiceGuid, String accountType, Long knownTotalCents) {
        long total = knownTotalCents != null ? knownTotalCents : computeInvoiceTotalCents(bookGuid, invoiceGuid);
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
        if (total <= 0) {
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
                " | 已结算金额分:" + (settled == null ? 0 : settled),
                invoiceGuid
        );
    }

    private long computeInvoiceTotalCents(String bookGuid, String invoiceGuid) {
        InvoiceCalc calc = loadInvoiceCalc(bookGuid, invoiceGuid);
        return calc.totalCents;
    }

    private InvoiceCalc loadInvoiceCalc(String bookGuid, String invoiceGuid) {
        List<InvoiceLine> lines = jdbcTemplate.query(
                """
                SELECT e.account_guid,
                       e.quantity_num, e.quantity_denom,
                       e.price_num, e.price_denom,
                       e.discount_num, e.discount_denom,
                       e.tax_table_guid,
                       e.taxable,
                       e.tax_included,
                       tt.rate_num,
                       tt.rate_denom,
                       tt.payable_account_guid,
                       tt.direction
                  FROM entries e
                  LEFT JOIN taxtables tt ON e.tax_table_guid = tt.guid
                 WHERE e.invoice_guid = ? AND e.book_guid = ?
                """,
                (rs, i) -> new InvoiceLine(
                        rs.getString("account_guid"),
                        rs.getLong("quantity_num"),
                        rs.getLong("quantity_denom"),
                        rs.getLong("price_num"),
                        rs.getLong("price_denom"),
                        rs.getObject("discount_num") != null ? rs.getLong("discount_num") : null,
                        rs.getObject("discount_denom") != null ? rs.getLong("discount_denom") : null,
                        rs.getString("tax_table_guid"),
                        rs.getInt("taxable"),
                        rs.getInt("tax_included"),
                        rs.getObject("rate_num") != null ? rs.getLong("rate_num") : null,
                        rs.getObject("rate_denom") != null ? rs.getLong("rate_denom") : null,
                        rs.getString("payable_account_guid"),
                        rs.getString("direction")
                ),
                invoiceGuid,
                bookGuid
        );

        if (lines.isEmpty()) {
            throw new IllegalStateException("发票未找到明细行，无法过账");
        }

        Map<String, Long> baseByAccount = new HashMap<>();
        Map<String, Long> taxByAccount = new HashMap<>();
        long totalCents = 0;

        for (InvoiceLine line : lines) {
            BigDecimal qty = toDecimal(line.quantityNum, line.quantityDenom);
            BigDecimal price = toDecimal(line.priceNum, line.priceDenom);
            BigDecimal discount = toDecimal(line.discountNum, line.discountDenom);

            BigDecimal gross = qty.multiply(price);
            BigDecimal net = gross.subtract(discount);
            if (net.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("明细金额不能为负数");
            }

            BigDecimal rate = toDecimal(line.rateNum, line.rateDenom);
            boolean taxable = line.taxable != null && line.taxable == 1 && line.taxTableGuid != null;
            if (taxable && rate.compareTo(BigDecimal.ZERO) <= 0) {
                taxable = false;
            }
            if (taxable && line.payableAccountGuid == null) {
                throw new IllegalArgumentException("税表未配置税额挂账科目");
            }
            if (taxable && !"INPUT".equalsIgnoreCase(coalesce(line.direction, ""))) {
                throw new IllegalArgumentException("采购发票税表方向必须为 INPUT");
            }

            BigDecimal base;
            BigDecimal tax = BigDecimal.ZERO;
            if (taxable) {
                if (line.taxIncluded != null && line.taxIncluded == 1) {
                    base = net.divide(BigDecimal.ONE.add(rate), 2, RoundingMode.HALF_UP);
                    tax = net.subtract(base);
                } else {
                    base = net;
                    tax = net.multiply(rate).setScale(2, RoundingMode.HALF_UP);
                }
            } else {
                base = net;
            }

            long baseCents = toCents(base);
            long taxCents = toCents(tax);
            baseByAccount.merge(line.accountGuid, baseCents, Long::sum);
            if (taxCents != 0 && line.payableAccountGuid != null) {
                taxByAccount.merge(line.payableAccountGuid, taxCents, Long::sum);
            }
            totalCents += baseCents + taxCents;
        }

        return new InvoiceCalc(baseByAccount, taxByAccount, totalCents);
    }

    private BigDecimal toDecimal(Long num, Long denom) {
        if (num == null) {
            return BigDecimal.ZERO;
        }
        long safeDenom = (denom == null || denom == 0) ? 1L : denom;
        return BigDecimal.valueOf(num).divide(BigDecimal.valueOf(safeDenom), 6, RoundingMode.HALF_UP);
    }

    private long toCents(BigDecimal amount) {
        return amount.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).longValue();
    }

    private record InvoiceCalc(Map<String, Long> baseByAccount, Map<String, Long> taxByAccount, long totalCents) {
    }

    private record InvoiceLine(
            String accountGuid,
            long quantityNum,
            long quantityDenom,
            long priceNum,
            long priceDenom,
            Long discountNum,
            Long discountDenom,
            String taxTableGuid,
            Integer taxable,
            Integer taxIncluded,
            Long rateNum,
            Long rateDenom,
            String payableAccountGuid,
            String direction
    ) {
    }
}
