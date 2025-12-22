package com.moon.backend.service;

import com.moon.backend.dto.BalanceSheetResponse;
import com.moon.backend.dto.CashFlowResponse;
import com.moon.backend.dto.NamedAmount;
import com.moon.backend.dto.ProfitLossResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final JdbcTemplate jdbcTemplate;

    public ProfitLossResponse profitLoss(String bookGuid, LocalDate start, LocalDate end) {
        List<AccountBalance> balances = queryBalances(bookGuid, start, end);
        Map<String, BigDecimal> income = new HashMap<>();
        Map<String, BigDecimal> expense = new HashMap<>();

        for (AccountBalance ab : balances) {
            String lower = ab.name.toLowerCase(Locale.ROOT);
            if ("INCOME".equalsIgnoreCase(ab.type)) {
                String bucket = (lower.contains("主营") || lower.contains("销售")) ? "主营业务收入" : "其他业务收入";
                income.merge(bucket, ab.amount, BigDecimal::add);
            } else if ("EXPENSE".equalsIgnoreCase(ab.type)) {
                String bucket;
                if (lower.contains("成本")) {
                    bucket = "主营业务成本";
                } else if (lower.contains("销售费用")) {
                    bucket = "销售费用";
                } else if (lower.contains("管理费用")) {
                    bucket = "管理费用";
                } else if (lower.contains("财务费用")) {
                    bucket = "财务费用";
                } else {
                    bucket = "其他费用";
                }
                expense.merge(bucket, ab.amount, BigDecimal::add);
            }
        }

        List<NamedAmount> incomeItems = toList(income);
        List<NamedAmount> expenseItems = toList(expense);
        BigDecimal totalIncome = sumAmounts(incomeItems);
        BigDecimal totalExpense = sumAmounts(expenseItems);
        BigDecimal netProfit = totalIncome.subtract(totalExpense);

        return new ProfitLossResponse(incomeItems, expenseItems, totalIncome, totalExpense, netProfit);
    }

    public BalanceSheetResponse balanceSheet(String bookGuid, LocalDate asOf) {
        List<AccountBalance> balances = queryBalances(bookGuid, null, asOf);

        Map<String, BigDecimal> asset = new HashMap<>();
        Map<String, BigDecimal> liability = new HashMap<>();
        Map<String, BigDecimal> equity = new HashMap<>();

        for (AccountBalance ab : balances) {
            String lower = ab.name.toLowerCase(Locale.ROOT);
            switch (ab.type.toUpperCase(Locale.ROOT)) {
                case "ASSET" -> {
                    String bucket;
                    if (lower.contains("现金") || lower.contains("银行存款")) {
                        bucket = "货币资金";
                    } else if (lower.contains("应收账款")) {
                        bucket = "应收账款";
                    } else if (lower.contains("预付")) {
                        bucket = "预付账款";
                    } else if (lower.contains("应收")) {
                        bucket = "其他应收";
                    } else if (lower.contains("存货")) {
                        bucket = "存货";
                    } else if (lower.contains("固定资产")) {
                        bucket = "固定资产";
                    } else if (lower.contains("累计折旧")) {
                        bucket = "累计折旧";
                    } else {
                        bucket = "其他资产";
                    }
                    asset.merge(bucket, ab.amount, BigDecimal::add);
                }
                case "LIABILITY" -> {
                    String bucket;
                    if (lower.contains("应付账款")) {
                        bucket = "应付账款";
                    } else if (lower.contains("预收")) {
                        bucket = "预收账款";
                    } else if (lower.contains("应付职工薪酬")) {
                        bucket = "应付职工薪酬";
                    } else if (lower.contains("应交") || lower.contains("税")) {
                        bucket = "应交税费";
                    } else {
                        bucket = "其他负债";
                    }
                    liability.merge(bucket, ab.amount, BigDecimal::add);
                }
                case "EQUITY" -> {
                    String bucket;
                    if (lower.contains("实收资本") || lower.contains("股本")) {
                        bucket = "实收资本";
                    } else if (lower.contains("资本公积")) {
                        bucket = "资本公积";
                    } else if (lower.contains("未分配利润")) {
                        bucket = "未分配利润";
                    } else {
                        bucket = "其他权益";
                    }
                    equity.merge(bucket, ab.amount, BigDecimal::add);
                }
                default -> {
                }
            }
        }

        List<NamedAmount> assetItems = toList(asset);
        List<NamedAmount> liabilityItems = toList(liability);
        List<NamedAmount> equityItems = toList(equity);
        BigDecimal totalAssets = sumAmounts(assetItems);
        BigDecimal totalLiabilities = sumAmounts(liabilityItems);
        BigDecimal totalEquity = sumAmounts(equityItems);

        return new BalanceSheetResponse(
                assetItems,
                liabilityItems,
                equityItems,
                totalAssets,
                totalLiabilities,
                totalEquity
        );
    }

    public CashFlowResponse cashFlowNet(String bookGuid, LocalDate start, LocalDate end) {
        List<AccountBalance> beginBalances = queryBalances(bookGuid, null, start.minusDays(1));
        List<AccountBalance> periodBalances = queryBalances(bookGuid, start, end);
        BigDecimal begin = sumCashLike(beginBalances);
        BigDecimal change = sumCashLike(periodBalances);
        BigDecimal endBalance = begin.add(change);
        return new CashFlowResponse(begin, endBalance, change);
    }

    private List<AccountBalance> queryBalances(String bookGuid, LocalDate start, LocalDate end) {
        StringBuilder sql = new StringBuilder("""
                SELECT a.guid,
                       a.name,
                       a.account_type,
                       SUM(CAST(s.value_num AS DECIMAL(18,4)) / NULLIF(s.value_denom,0)) AS amount
                  FROM splits s
                  JOIN transactions t ON s.tx_guid = t.guid
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE t.book_guid = ?
                """);
        List<Object> args = new ArrayList<>();
        args.add(bookGuid);
        if (start != null) {
            sql.append(" AND t.post_date >= ? ");
            args.add(start.atStartOfDay());
        }
        if (end != null) {
            sql.append(" AND t.post_date <= ? ");
            args.add(end.plusDays(1).atStartOfDay().minusNanos(1));
        }
        sql.append(" GROUP BY a.guid, a.name, a.account_type");

        return jdbcTemplate.query(sql.toString(), (rs, i) -> {
            BigDecimal amt = rs.getBigDecimal("amount");
            if (amt == null) {
                amt = BigDecimal.ZERO;
            }
            return new AccountBalance(
                    rs.getString("name"),
                    rs.getString("account_type"),
                    amt.setScale(2, RoundingMode.HALF_UP)
            );
        }, args.toArray());
    }

    private List<NamedAmount> toList(Map<String, BigDecimal> map) {
        return map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> new NamedAmount(e.getKey(), e.getValue().setScale(2, RoundingMode.HALF_UP)))
                .collect(Collectors.toList());
    }

    private BigDecimal sumAmounts(List<NamedAmount> items) {
        return items.stream()
                .map(NamedAmount::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal sumCashLike(List<AccountBalance> balances) {
        BigDecimal sum = BigDecimal.ZERO;
        for (AccountBalance ab : balances) {
            String lower = ab.name.toLowerCase(Locale.ROOT);
            if (lower.contains("现金") || lower.contains("银行存款")) {
                sum = sum.add(ab.amount);
            }
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private record AccountBalance(String name, String type, BigDecimal amount) {
    }
}
