package com.moon.backend.service;

import com.moon.backend.dto.TaxCalcRequest;
import com.moon.backend.dto.TaxCalcResponse;
import com.moon.backend.dto.TaxPostRequest;
import com.moon.backend.dto.TaxRateRequest;
import com.moon.backend.dto.TaxRateResponse;
import com.moon.backend.entity.Account;
import com.moon.backend.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaxService {

    private final JdbcTemplate jdbcTemplate;
    private final AccountRepository accountRepository;

    public List<TaxRateResponse> listRates(String bookGuid) {
        return jdbcTemplate.query(
                "SELECT guid, name, direction, rate_num, rate_denom, payable_account_guid, description, active " +
                        "FROM taxtables WHERE book_guid = ? ORDER BY name",
                (rs, i) -> new TaxRateResponse(
                        rs.getString("guid"),
                        rs.getString("name"),
                        toPercent(rs.getLong("rate_num"), rs.getLong("rate_denom")),
                        rs.getString("direction"),
                        rs.getString("payable_account_guid"),
                        rs.getString("description"),
                        rs.getBoolean("active")
                ),
                bookGuid
        );
    }

    @Transactional
    public String createRate(TaxRateRequest request) {
        String guid = UUID.randomUUID().toString();
        long rateNum = request.getRatePercent().longValue();
        long rateDenom = 100L;
        jdbcTemplate.update(
                "INSERT INTO taxtables (guid, book_guid, name, direction, rate_num, rate_denom, payable_account_guid, description, active) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                guid,
                request.getBookGuid(),
                request.getName(),
                request.getDirection().toUpperCase(),
                rateNum,
                rateDenom,
                request.getPayableAccountGuid(),
                request.getDescription()
        );
        return guid;
    }

    public TaxCalcResponse calculate(TaxCalcRequest request) {
        double rate = request.getRatePercent() != null ? request.getRatePercent() : loadRatePercent(request.getTaxTableGuid());
        long base = request.getAmountCent();
        long tax = calcTax(base, rate);
        long total = base + tax;
        return new TaxCalcResponse(base, tax, total, rate, null);
    }

    @Transactional
    public String postTax(TaxPostRequest request) {
        // 校验基础科目和税额挂账科目
        Account baseAccount = accountRepository.findById(request.getBaseAccountGuid())
                .orElseThrow(() -> new IllegalArgumentException("基础科目不存在"));
        String direction = jdbcTemplate.queryForObject(
                "SELECT direction FROM taxtables WHERE guid = ?",
                String.class,
                request.getTaxTableGuid()
        );
        if (direction == null) {
            throw new IllegalArgumentException("税率不存在");
        }
        String payableAccountGuid = jdbcTemplate.queryForObject(
                "SELECT payable_account_guid FROM taxtables WHERE guid = ?",
                String.class,
                request.getTaxTableGuid()
        );
        if (payableAccountGuid == null) {
            throw new IllegalArgumentException("税率未配置挂账科目");
        }
        long tax = calcTax(request.getAmountCent(), loadRatePercent(request.getTaxTableGuid()));
        long base = request.getAmountCent();
        long total = base + tax;
        String cashGuid = resolveCashAccount(request.getBookGuid(), request.getCashAccountGuid());

        LocalDateTime now = request.getPostDate() != null ? request.getPostDate() : LocalDateTime.now();
        String txGuid = UUID.randomUUID().toString();
        jdbcTemplate.update(
                "INSERT INTO transactions (guid, book_guid, num, post_date, enter_date, description, doc_status, source_type, source_guid) " +
                        "VALUES (?, ?, NULL, ?, ?, ?, 'POSTED', 'TAX_MANUAL', ?)",
                txGuid,
                request.getBookGuid(),
                now,
                now,
                request.getDescription(),
                request.getTaxTableGuid()
        );

        if ("INPUT".equalsIgnoreCase(direction)) {
            insertSplit(txGuid, baseAccount.getGuid(), base, request.getDescription());
            insertSplit(txGuid, payableAccountGuid, tax, "进项税额");
            insertSplit(txGuid, cashGuid, -total, "付款");
        } else {
            insertSplit(txGuid, cashGuid, total, "收款");
            insertSplit(txGuid, baseAccount.getGuid(), -base, request.getDescription());
            insertSplit(txGuid, payableAccountGuid, -tax, "销项税额");
        }
        return txGuid;
    }

    private String resolveCashAccount(String bookGuid, String preferGuidOrName) {
        if (preferGuidOrName != null && !preferGuidOrName.isBlank()) {
            return accountRepository.findById(preferGuidOrName)
                    .map(Account::getGuid)
                    .or(() -> accountRepository.findFirstByBookGuidAndName(bookGuid, preferGuidOrName).map(Account::getGuid))
                    .orElseThrow(() -> new IllegalArgumentException("未找到现金科目：" + preferGuidOrName));
        }
        return accountRepository.findFirstByBookGuidAndName(bookGuid, "银行存款")
                .map(Account::getGuid)
                .orElseThrow(() -> new IllegalStateException("未找到“银行存款”科目"));
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

    private double loadRatePercent(String taxTableGuid) {
        Double value = jdbcTemplate.query(
                "SELECT rate_num, rate_denom FROM taxtables WHERE guid = ?",
                rs -> rs.next() ? rs.getDouble("rate_num") * 100.0 / rs.getDouble("rate_denom") : null,
                taxTableGuid
        );
        if (value == null) {
            throw new IllegalArgumentException("税率不存在");
        }
        return value;
    }

    private long calcTax(long base, double ratePercent) {
        BigDecimal tax = BigDecimal.valueOf(base).multiply(BigDecimal.valueOf(ratePercent))
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        return tax.longValue();
    }

    private double toPercent(long num, long denom) {
        if (denom == 0) {
            return 0d;
        }
        return BigDecimal.valueOf(num).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }
}

