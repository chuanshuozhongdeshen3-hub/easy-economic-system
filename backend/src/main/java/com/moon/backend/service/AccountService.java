package com.moon.backend.service;

import com.moon.backend.dto.AccountNodeResponse;
import com.moon.backend.dto.CreateAccountRequest;
import com.moon.backend.dto.UpdateAccountRequest;
import com.moon.backend.entity.Account;
import com.moon.backend.entity.Book;
import com.moon.backend.repository.AccountRepository;
import com.moon.backend.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BookRepository bookRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<AccountNodeResponse> getAccountTree(String bookGuid) {
        List<Account> accounts = accountRepository.findByBookGuid(bookGuid);
        if (accounts.isEmpty()) {
            // 兜底：账本存在但没有科目时，自动创建根账户和默认科目
            createDefaultAccountsIfMissing(bookGuid);
            accounts = accountRepository.findByBookGuid(bookGuid);
        }
        Map<String, BigDecimal> baseBalances = loadBaseBalances(bookGuid);
        applyRegisteredCapital(bookGuid, baseBalances);

        Map<String, AccountNodeResponse> nodeMap = new HashMap<>();
        for (Account account : accounts) {
            AccountNodeResponse node = new AccountNodeResponse();
            node.setGuid(account.getGuid());
            node.setName(account.getName());
            node.setCode(account.getCode());
            node.setAccountType(account.getAccountType());
            node.setDescription(account.getDescription());
            node.setBalance(baseBalances.getOrDefault(account.getGuid(), BigDecimal.ZERO));
            nodeMap.put(account.getGuid(), node);
        }

        // 构建树
        List<AccountNodeResponse> roots = new ArrayList<>();
        for (Account account : accounts) {
            AccountNodeResponse node = nodeMap.get(account.getGuid());
            if (account.getParentGuid() == null) {
                roots.add(node);
            } else {
                AccountNodeResponse parent = nodeMap.get(account.getParentGuid());
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node); // 兜底：如果缺失父节点，作为根节点
                }
            }
        }

        // 排序（按名称/编码），并向上汇总余额
        for (AccountNodeResponse root : roots) {
            sortChildren(root);
            aggregateBalance(root);
        }

        // 不暴露根占位科目，返回其子科目列表
        List<AccountNodeResponse> visibleRoots = roots.stream()
                .flatMap(r -> r.getChildren().isEmpty() ? List.of(r).stream() : r.getChildren().stream())
                .collect(Collectors.toList());
        return visibleRoots;
    }

    private void sortChildren(AccountNodeResponse node) {
        node.getChildren().sort(Comparator.comparing((AccountNodeResponse n) -> Objects.toString(n.getCode(), ""))
                .thenComparing(AccountNodeResponse::getName));
        for (AccountNodeResponse child : node.getChildren()) {
            sortChildren(child);
        }
    }

    private BigDecimal aggregateBalance(AccountNodeResponse node) {
        BigDecimal sum = node.getBalance();
        for (AccountNodeResponse child : node.getChildren()) {
            sum = sum.add(aggregateBalance(child));
        }
        node.setBalance(sum);
        return sum;
    }

    private Map<String, BigDecimal> loadBaseBalances(String bookGuid) {
        String sql = """
                SELECT s.account_guid AS guid,
                       SUM(CAST(s.value_num AS DECIMAL(18,4)) / NULLIF(s.value_denom, 0)) AS balance
                  FROM splits s
                  JOIN transactions t ON s.tx_guid = t.guid
                 WHERE t.book_guid = ?
                 GROUP BY s.account_guid
                """;
        Map<String, BigDecimal> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            BigDecimal balance = rs.getBigDecimal("balance");
            if (balance != null) {
                map.put(rs.getString("guid"), balance.setScale(2, RoundingMode.HALF_UP));
            }
        }, bookGuid);
        return map;
    }

    /**
     * 如果账本配置了注册资本，则将金额挂到“实收资本”和“银行存款”科目上（用于初始展示）。
     */
    private void applyRegisteredCapital(String bookGuid, Map<String, BigDecimal> baseBalances) {
        bookRepository.findById(bookGuid).ifPresent(book -> {
            Long num = book.getRegisteredCapitalNum();
            Long denom = book.getRegisteredCapitalDenom();
            if (num == null || num == 0) {
                return;
            }
            // 默认按元为单位；历史版本分母默认为 100，这里按 1 处理以避免显示被缩小 100 倍
            if (denom == null || denom <= 0 || denom == 100L) {
                denom = 1L;
            }
            BigDecimal value = BigDecimal.valueOf(num)
                    .divide(BigDecimal.valueOf(denom), 2, RoundingMode.HALF_UP);

            accountRepository.findFirstByBookGuidAndName(bookGuid, "实收资本")
                    .ifPresent(account -> baseBalances.merge(account.getGuid(), value, BigDecimal::add));
            // 同时放入银行存款，便于资产负债表平衡展示
            accountRepository.findFirstByBookGuidAndName(bookGuid, "银行存款")
                    .ifPresent(account -> baseBalances.merge(account.getGuid(), value, BigDecimal::add));
        });
    }

    @Transactional
    public Account createAccount(CreateAccountRequest request) {
        Account parent = accountRepository.findById(request.getParentGuid())
                .orElseThrow(() -> new IllegalArgumentException("父级科目不存在"));
        if (!Objects.equals(parent.getBookGuid(), request.getBookGuid())) {
            throw new IllegalArgumentException("父级科目不属于当前账本");
        }
        if (!Objects.equals(parent.getAccountType(), request.getAccountType())) {
            throw new IllegalArgumentException("子科目的会计科目类别必须与父级一致");
        }

        Account account = new Account();
        account.setGuid(UUID.randomUUID().toString());
        account.setBookGuid(request.getBookGuid());
        account.setName(request.getName());
        account.setCode(request.getCode());
        account.setDescription(request.getDescription());
        account.setAccountType(request.getAccountType());
        account.setParentGuid(request.getParentGuid());
        account.setHidden(false);
        account.setPlaceholder(false);
        account.setCreatedAt(LocalDateTime.now());
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    @Transactional
    public Account updateAccount(UpdateAccountRequest request) {
        Account account = accountRepository.findById(request.getGuid())
                .orElseThrow(() -> new IllegalArgumentException("科目不存在"));
        if (request.getName() != null && !request.getName().isBlank()) {
            account.setName(request.getName());
        }
        if (request.getCode() != null) {
            account.setCode(request.getCode());
        }
        if (request.getDescription() != null) {
            account.setDescription(request.getDescription());
        }
        account.setUpdatedAt(LocalDateTime.now());
        return accountRepository.save(account);
    }

    @Transactional
    public void deleteAccount(String guid) {
        Account account = accountRepository.findById(guid)
                .orElseThrow(() -> new IllegalArgumentException("科目不存在"));

        if (accountRepository.existsByParentGuid(guid)) {
            throw new IllegalStateException("存在下级科目，无法删除");
        }

        // 检查凭证分录和业务行是否使用
        Long splitCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(1)
                          FROM splits s
                          JOIN transactions t ON s.tx_guid = t.guid
                         WHERE s.account_guid = ? AND t.book_guid = ?
                        """,
                Long.class,
                guid,
                account.getBookGuid()
        );
        if (splitCount != null && splitCount > 0) {
            throw new IllegalStateException("科目已在凭证分录中使用，无法删除");
        }

        Long entryCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM entries WHERE account_guid = ?",
                Long.class,
                guid
        );
        if (entryCount != null && entryCount > 0) {
            throw new IllegalStateException("科目已在业务分录中使用，无法删除");
        }

        accountRepository.delete(account);
    }

    /**
     * 查询科目关联的订单/发票/交易（按日期倒序）。
     */
    public List<com.moon.backend.dto.RelatedDocResponse> listRelatedDocs(String bookGuid, String accountGuid, boolean includeChildren) {
        List<String> guids = resolveAccountAndChildren(bookGuid, accountGuid, includeChildren);
        if (guids.isEmpty()) {
            return List.of();
        }
        String placeholders = guids.stream().map(g -> "?").collect(Collectors.joining(","));
        String sql = """
                SELECT doc_type, doc_id, doc_date, description FROM (
                  SELECT 'INVOICE' AS doc_type, i.id AS doc_id, i.date_opened AS doc_date, COALESCE(i.notes,'') AS description
                    FROM entries e
                    JOIN invoices i ON e.invoice_guid = i.guid
                   WHERE e.book_guid = ? AND e.account_guid IN (%s)
                  UNION ALL
                  SELECT 'ORDER' AS doc_type, o.id AS doc_id, o.date_opened AS doc_date, COALESCE(o.notes,'') AS description
                    FROM entries e
                    JOIN orders o ON e.order_guid = o.guid
                   WHERE e.book_guid = ? AND e.account_guid IN (%s)
                  UNION ALL
                  SELECT 'TRANSACTION' AS doc_type, COALESCE(t.num, t.guid) AS doc_id, t.post_date AS doc_date, COALESCE(t.description,'') AS description
                    FROM splits s
                    JOIN transactions t ON s.tx_guid = t.guid
                   WHERE t.book_guid = ? AND s.account_guid IN (%s)
                ) AS doc
                ORDER BY doc_date DESC
                """.formatted(placeholders, placeholders, placeholders);
        List<Object> args = new ArrayList<>();
        args.add(bookGuid);
        args.addAll(guids);
        args.add(bookGuid);
        args.addAll(guids);
        args.add(bookGuid);
        args.addAll(guids);

        List<com.moon.backend.dto.RelatedDocResponse> list = jdbcTemplate.query(sql, (rs, i) -> new com.moon.backend.dto.RelatedDocResponse(
                rs.getString("doc_type"),
                rs.getString("doc_id"),
                rs.getTimestamp("doc_date") != null ? rs.getTimestamp("doc_date").toLocalDateTime() : null,
                rs.getString("description")
        ), args.toArray());

        // 附加注册资本提示：资产/所有者权益时
        bookRepository.findById(bookGuid).ifPresent(book -> {
            boolean isAssetOrEquityRoot = guids.contains(resolveByName(bookGuid, "资产").map(Account::getGuid).orElse("___"))
                    || guids.contains(resolveByName(bookGuid, "所有者权益").map(Account::getGuid).orElse("___"));
            if (isAssetOrEquityRoot) {
                Long num = book.getRegisteredCapitalNum();
                Long denom = book.getRegisteredCapitalDenom();
                if (num != null && num > 0) {
                    long safeDenom = denom == null || denom <= 0 ? 1L : denom;
                    BigDecimal capital = BigDecimal.valueOf(num).divide(BigDecimal.valueOf(safeDenom), 2, RoundingMode.HALF_UP);
                    list.add(0, new com.moon.backend.dto.RelatedDocResponse(
                            "REGISTERED_CAPITAL",
                            "注册资本",
                            null,
                            "注册资本金额：¥" + capital
                    ));
                }
            }
        });
        return list;
    }

    private Optional<Account> resolveByName(String bookGuid, String name) {
        return accountRepository.findFirstByBookGuidAndName(bookGuid, name);
    }

    private List<String> resolveAccountAndChildren(String bookGuid, String guid, boolean includeChildren) {
        if (!includeChildren) {
            return List.of(guid);
        }
        return jdbcTemplate.queryForList(
                """
                        WITH RECURSIVE sub AS (
                            SELECT guid, parent_guid FROM accounts WHERE guid = ? AND book_guid = ?
                            UNION ALL
                            SELECT a.guid, a.parent_guid
                              FROM accounts a
                              JOIN sub s ON a.parent_guid = s.guid
                             WHERE a.book_guid = ?
                        )
                        SELECT guid FROM sub
                        """,
                String.class,
                guid,
                bookGuid,
                bookGuid
        );
    }

    @Transactional
    protected void createDefaultAccountsIfMissing(String bookGuid) {
        Book book = bookRepository.findById(bookGuid)
                .orElseThrow(() -> new IllegalArgumentException("账本不存在"));

        String rootGuid = book.getRootAccountGuid();
        if (rootGuid == null || !accountRepository.existsById(rootGuid)) {
            rootGuid = UUID.randomUUID().toString();
            jdbcTemplate.update("UPDATE books SET root_account_guid = ? WHERE guid = ?", rootGuid, bookGuid);
            insertAccount(rootGuid, bookGuid, "根账户", "0", "ASSET", null, true, "系统自动创建的根账户", LocalDateTime.now());
        }

        boolean hasChildren = accountRepository.existsByParentGuid(rootGuid);
        if (!hasChildren) {
            seedDefaultAccounts(bookGuid, rootGuid, LocalDateTime.now());
        }
    }

    private void seedDefaultAccounts(String bookGuid, String rootGuid, LocalDateTime now) {
        String assetGuid = UUID.randomUUID().toString();
        String liabilityGuid = UUID.randomUUID().toString();
        String equityGuid = UUID.randomUUID().toString();
        String incomeGuid = UUID.randomUUID().toString();
        String expenseGuid = UUID.randomUUID().toString();

        insertAccount(assetGuid, bookGuid, "资产", "1", "ASSET", rootGuid, true, "资产类科目", now);
        insertAccount(liabilityGuid, bookGuid, "负债", "2", "LIABILITY", rootGuid, true, "负债类科目", now);
        insertAccount(equityGuid, bookGuid, "所有者权益", "3", "EQUITY", rootGuid, true, "权益类科目", now);
        insertAccount(incomeGuid, bookGuid, "收入", "4", "INCOME", rootGuid, true, "收入类科目", now);
        insertAccount(expenseGuid, bookGuid, "费用", "5", "EXPENSE", rootGuid, true, "费用类科目", now);

        List<AccountSeed> seeds = new ArrayList<>();
        seeds.add(new AccountSeed("现金", "1001", "ASSET", assetGuid, false, "库存现金"));
        seeds.add(new AccountSeed("银行存款", "1002", "ASSET", assetGuid, false, "银行账户余额"));
        seeds.add(new AccountSeed("应收账款", "1122", "ASSET", assetGuid, false, "客户应收"));
        seeds.add(new AccountSeed("预付账款", "1123", "ASSET", assetGuid, false, "供应商预付款"));
        seeds.add(new AccountSeed("存货", "1401", "ASSET", assetGuid, false, "原材料/库存商品"));
        seeds.add(new AccountSeed("固定资产", "1601", "ASSET", assetGuid, false, "固定资产原值"));
        seeds.add(new AccountSeed("累计折旧", "1602", "ASSET", assetGuid, false, "固定资产累计折旧"));

        seeds.add(new AccountSeed("应付账款", "2202", "LIABILITY", liabilityGuid, false, "供应商应付"));
        seeds.add(new AccountSeed("预收账款", "2203", "LIABILITY", liabilityGuid, false, "客户预收"));
        seeds.add(new AccountSeed("应付职工薪酬", "2211", "LIABILITY", liabilityGuid, false, "工资社保公积金"));
        seeds.add(new AccountSeed("应交税费", "2221", "LIABILITY", liabilityGuid, false, "各类税费应交"));

        seeds.add(new AccountSeed("实收资本", "3001", "EQUITY", equityGuid, false, "投资者投入资本"));
        seeds.add(new AccountSeed("资本公积", "3002", "EQUITY", equityGuid, false, "资本溢价等"));
        seeds.add(new AccountSeed("留存收益", "3101", "EQUITY", equityGuid, false, "未分配利润"));

        seeds.add(new AccountSeed("主营业务收入", "6001", "INCOME", incomeGuid, false, "主营产品/服务收入"));
        seeds.add(new AccountSeed("其他业务收入", "6051", "INCOME", incomeGuid, false, "非主营业务收入"));

        seeds.add(new AccountSeed("主营业务成本", "6401", "EXPENSE", expenseGuid, false, "对应主营收入的成本"));
        seeds.add(new AccountSeed("销售费用", "6601", "EXPENSE", expenseGuid, false, "销售相关费用"));
        seeds.add(new AccountSeed("管理费用", "6602", "EXPENSE", expenseGuid, false, "管理相关费用"));
        seeds.add(new AccountSeed("财务费用", "6603", "EXPENSE", expenseGuid, false, "利息等财务成本"));

        for (AccountSeed seed : seeds) {
            insertAccount(UUID.randomUUID().toString(), bookGuid, seed.name, seed.code, seed.type, seed.parentGuid, seed.placeholder, seed.description, now);
        }
    }

    private void insertAccount(String guid, String bookGuid, String name, String code, String type, String parentGuid, boolean placeholder, String description, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO accounts (guid, book_guid, name, code, description, account_type, parent_guid, hidden, placeholder, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, 0, ?, ?, ?)",
                guid,
                bookGuid,
                name,
                code,
                description,
                type,
                parentGuid,
                placeholder ? 1 : 0,
                now,
                now
        );
    }

    private static class AccountSeed {
        final String name;
        final String code;
        final String type;
        final String parentGuid;
        final boolean placeholder;
        final String description;

        AccountSeed(String name, String code, String type, String parentGuid, boolean placeholder, String description) {
            this.name = name;
            this.code = code;
            this.type = type;
            this.parentGuid = parentGuid;
            this.placeholder = placeholder;
            this.description = description;
        }
    }

    /**
     * 按科目类型汇总余额，用于平衡性校验
     */
    public Map<String, BigDecimal> sumByType(String bookGuid) {
        String sql = """
                SELECT a.account_type,
                       COALESCE(SUM(CAST(s.value_num AS DECIMAL(18,4)) / NULLIF(s.value_denom,0)),0) AS total
                  FROM splits s
                  JOIN transactions t ON s.tx_guid = t.guid
                  JOIN accounts a ON s.account_guid = a.guid
                 WHERE t.book_guid = ?
                 GROUP BY a.account_type
                """;
        Map<String, BigDecimal> map = new HashMap<>();
        jdbcTemplate.query(sql, rs -> {
            map.put(rs.getString("account_type"), rs.getBigDecimal("total").setScale(2, RoundingMode.HALF_UP));
        }, bookGuid);
        return map;
    }
}
