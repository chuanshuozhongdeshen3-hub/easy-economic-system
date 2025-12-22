package com.moon.backend.service;

import com.moon.backend.dto.AccountNodeResponse;
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
    private final AccountService accountService;

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

        // 树形：只取收入/费用两棵
        List<AccountNodeResponse> tree = accountService.getAccountTree(bookGuid);
        List<AccountNodeResponse> incomeTree = tree.stream()
                .filter(n -> "INCOME".equalsIgnoreCase(n.getAccountType()))
                .toList();
        List<AccountNodeResponse> expenseTree = tree.stream()
                .filter(n -> "EXPENSE".equalsIgnoreCase(n.getAccountType()))
                .toList();

        return new ProfitLossResponse(incomeItems, expenseItems, totalIncome, totalExpense, netProfit, incomeTree, expenseTree);
    }

    public BalanceSheetResponse balanceSheet(String bookGuid, LocalDate asOf) {
        try {
            // 使用账户树直接分组展示
            List<AccountNodeResponse> tree = accountService.getAccountTree(bookGuid);
            AccountNodeResponse assetRoot = findTop(tree, "ASSET");
            AccountNodeResponse liabRoot = findTop(tree, "LIABILITY");
            AccountNodeResponse equityRoot = findTop(tree, "EQUITY");

            BigDecimal totalAssets = safeBalance(assetRoot);
            BigDecimal totalLiabilities = safeBalance(liabRoot);
            BigDecimal totalEquity = safeBalance(equityRoot);

            return new BalanceSheetResponse(
                    assetRoot != null ? assetRoot.getChildren() : List.of(),
                    liabRoot != null ? liabRoot.getChildren() : List.of(),
                    equityRoot != null ? equityRoot.getChildren() : List.of(),
                    totalAssets,
                    totalLiabilities,
                    totalEquity
            );
        } catch (Exception ex) {
            throw new IllegalStateException("资产负债表计算失败: " + ex.getClass().getSimpleName() + " - " + ex.getMessage(), ex);
        }
    }

    public CashFlowResponse cashFlowNet(String bookGuid, LocalDate start, LocalDate end) {
        List<AccountBalance> beginBalances = queryBalances(bookGuid, null, start.minusDays(1));
        List<AccountBalance> periodBalances = queryBalances(bookGuid, start, end);
        BigDecimal begin = sumCashLike(beginBalances);
        BigDecimal change = sumCashLike(periodBalances);
        BigDecimal endBalance = begin.add(change);

        // 按本期科目余额正负拆分资金来源/流出（简单版）
        List<NamedAmount> inflow = new ArrayList<>();
        List<NamedAmount> outflow = new ArrayList<>();
        for (AccountBalance ab : periodBalances) {
            if (ab.amount.compareTo(BigDecimal.ZERO) > 0) {
                inflow.add(new NamedAmount(ab.name, ab.amount.setScale(2, RoundingMode.HALF_UP)));
            } else if (ab.amount.compareTo(BigDecimal.ZERO) < 0) {
                outflow.add(new NamedAmount(ab.name, ab.amount.abs().setScale(2, RoundingMode.HALF_UP)));
            }
        }
        BigDecimal totalIn = sumAmounts(inflow);
        BigDecimal totalOut = sumAmounts(outflow);
        BigDecimal net = totalIn.subtract(totalOut);

        return new CashFlowResponse(begin, endBalance, net, inflow, outflow, totalIn, totalOut);
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

    private AccountNodeResponse findTop(List<AccountNodeResponse> roots, String type) {
        return roots.stream()
                .filter(n -> type.equalsIgnoreCase(n.getAccountType()))
                .findFirst()
                .orElse(null);
    }

    private BigDecimal safeBalance(AccountNodeResponse node) {
        return node == null || node.getBalance() == null
                ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : node.getBalance().setScale(2, RoundingMode.HALF_UP);
    }
}
