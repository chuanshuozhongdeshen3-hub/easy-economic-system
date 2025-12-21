package com.moon.backend.service;

import com.moon.backend.dto.AuthResponse;
import com.moon.backend.dto.LoginRequest;
import com.moon.backend.dto.RegisterRequest;
import com.moon.backend.entity.SysUser;
import com.moon.backend.entity.SysUserBook;
import com.moon.backend.repository.SysUserBookRepository;
import com.moon.backend.repository.SysUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserRepository userRepository;
    private final SysUserBookRepository userBookRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        validateRegisterRequest(request);

        SysUser sysUser = new SysUser();
        sysUser.setUsername(request.getUsername());
        sysUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        sysUser.setEnabled(true);
        sysUser.setCreatedAt(LocalDateTime.now());
        sysUser.setUpdatedAt(LocalDateTime.now());
        SysUser savedUser = userRepository.save(sysUser);

        String bookGuid = createDefaultBookForUser(savedUser.getId(), request);
        return new AuthResponse(savedUser.getId(), savedUser.getUsername(), bookGuid);
    }

    public AuthResponse login(LoginRequest request) {
        Optional<SysUser> userOptional = userRepository.findByUsername(request.getUsername());
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        SysUser user = userOptional.get();
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalStateException("账号已被禁用");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }

        String bookGuid = userBookRepository.findByUserId(user.getId())
                .map(SysUserBook::getBookGuid)
                .orElse(null);
        return new AuthResponse(user.getId(), user.getUsername(), bookGuid);
    }

    private void validateRegisterRequest(RegisterRequest request) {
        if (request.getRegisteredCapitalDenom() != null && request.getRegisteredCapitalDenom() <= 0) {
            throw new IllegalArgumentException("注册资本分母必须大于 0");
        }
        Integer startMonth = request.getFiscalYearStartMonth();
        Integer startDay = request.getFiscalYearStartDay();
        if (startMonth != null && (startMonth < 1 || startMonth > 12)) {
            throw new IllegalArgumentException("会计年度起始月份应在 1-12 之间");
        }
        if (startDay != null && (startDay < 1 || startDay > 31)) {
            throw new IllegalArgumentException("会计年度起始日应在 1-31 之间");
        }
    }

    private String createDefaultBookForUser(Long userId, RegisterRequest request) {
        String bookGuid = UUID.randomUUID().toString();
        String rootAccountGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        Long registeredCapitalNum = request.getRegisteredCapitalNum();
        Long registeredCapitalDenom = request.getRegisteredCapitalDenom();
        if (registeredCapitalDenom == null && registeredCapitalNum != null) {
            registeredCapitalDenom = 100L;
        }

        String disableForeignKeys = "SET FOREIGN_KEY_CHECKS=0";
        String enableForeignKeys = "SET FOREIGN_KEY_CHECKS=1";

        try {
            jdbcTemplate.execute(disableForeignKeys);
            jdbcTemplate.update(
                    "INSERT INTO books (guid, name, size, registered_capital_num, registered_capital_denom, root_account_guid, fiscal_year_start_month, fiscal_year_start_day, created_at, updated_at) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    bookGuid,
                    request.getBookName(),
                    request.getBookSize(),
                    registeredCapitalNum,
                    registeredCapitalDenom,
                    rootAccountGuid,
                    request.getFiscalYearStartMonth(),
                    request.getFiscalYearStartDay(),
                    now,
                    now
            );

            // 根账户
            jdbcTemplate.update(
                    "INSERT INTO accounts (guid, book_guid, name, code, description, account_type, parent_guid, hidden, placeholder, created_at, updated_at) " +
                            "VALUES (?, ?, ?, NULL, ?, ?, NULL, 0, 1, ?, ?)",
                    rootAccountGuid,
                    bookGuid,
                    "根账户",
                    "系统自动创建的根账户",
                    "ASSET",
                    now,
                    now
            );

            // 默认总账科目及示例子科目
            seedDefaultAccounts(bookGuid, rootAccountGuid, now);

            jdbcTemplate.update(
                    "INSERT INTO sys_user_books (user_id, book_guid, created_at) VALUES (?, ?, ?)",
                    userId,
                    bookGuid,
                    now
            );
        } finally {
            jdbcTemplate.execute(enableForeignKeys);
        }

        return bookGuid;
    }

    private void seedDefaultAccounts(String bookGuid, String rootGuid, LocalDateTime now) {
        // 总账科目
        String assetGuid = UUID.randomUUID().toString();
        String liabilityGuid = UUID.randomUUID().toString();
        String equityGuid = UUID.randomUUID().toString();
        String incomeGuid = UUID.randomUUID().toString();
        String expenseGuid = UUID.randomUUID().toString();

        insertAccount(assetGuid, bookGuid, "资产", "ASSET", rootGuid, true, "资产类科目", now);
        insertAccount(liabilityGuid, bookGuid, "负债", "LIABILITY", rootGuid, true, "负债类科目", now);
        insertAccount(equityGuid, bookGuid, "所有者权益", "EQUITY", rootGuid, true, "权益类科目", now);
        insertAccount(incomeGuid, bookGuid, "收入", "INCOME", rootGuid, true, "收入类科目", now);
        insertAccount(expenseGuid, bookGuid, "费用", "EXPENSE", rootGuid, true, "费用类科目", now);

        // 示例子科目
        List<AccountSeed> seeds = new ArrayList<>();
        seeds.add(new AccountSeed("现金", "ASSET", assetGuid, false, "库存现金"));
        seeds.add(new AccountSeed("银行存款", "ASSET", assetGuid, false, "银行账户余额"));
        seeds.add(new AccountSeed("应收账款", "ASSET", assetGuid, false, "客户应收"));
        seeds.add(new AccountSeed("预付账款", "ASSET", assetGuid, false, "供应商预付款"));
        seeds.add(new AccountSeed("存货", "ASSET", assetGuid, false, "原材料/库存商品"));
        seeds.add(new AccountSeed("固定资产", "ASSET", assetGuid, false, "固定资产原值"));
        seeds.add(new AccountSeed("累计折旧", "ASSET", assetGuid, false, "固定资产累计折旧"));

        seeds.add(new AccountSeed("应付账款", "LIABILITY", liabilityGuid, false, "供应商应付"));
        seeds.add(new AccountSeed("预收账款", "LIABILITY", liabilityGuid, false, "客户预收"));
        seeds.add(new AccountSeed("应付职工薪酬", "LIABILITY", liabilityGuid, false, "工资社保公积金"));
        seeds.add(new AccountSeed("应交税费", "LIABILITY", liabilityGuid, false, "各类税费应交"));

        seeds.add(new AccountSeed("实收资本", "EQUITY", equityGuid, false, "投资者投入资本"));
        seeds.add(new AccountSeed("资本公积", "EQUITY", equityGuid, false, "资本溢价等"));
        seeds.add(new AccountSeed("留存收益", "EQUITY", equityGuid, false, "未分配利润"));

        seeds.add(new AccountSeed("主营业务收入", "INCOME", incomeGuid, false, "主营产品/服务收入"));
        seeds.add(new AccountSeed("其他业务收入", "INCOME", incomeGuid, false, "非主营业务收入"));

        seeds.add(new AccountSeed("主营业务成本", "EXPENSE", expenseGuid, false, "对应主营收入的成本"));
        seeds.add(new AccountSeed("销售费用", "EXPENSE", expenseGuid, false, "销售相关费用"));
        seeds.add(new AccountSeed("管理费用", "EXPENSE", expenseGuid, false, "管理相关费用"));
        seeds.add(new AccountSeed("财务费用", "EXPENSE", expenseGuid, false, "利息等财务成本"));

        for (AccountSeed seed : seeds) {
            insertAccount(UUID.randomUUID().toString(), bookGuid, seed.name, seed.type, seed.parentGuid, seed.placeholder, seed.description, now);
        }
    }

    private void insertAccount(String guid, String bookGuid, String name, String type, String parentGuid, boolean placeholder, String description, LocalDateTime now) {
        jdbcTemplate.update(
                "INSERT INTO accounts (guid, book_guid, name, code, description, account_type, parent_guid, hidden, placeholder, created_at, updated_at) " +
                        "VALUES (?, ?, ?, NULL, ?, ?, ?, 0, ?, ?, ?)",
                guid,
                bookGuid,
                name,
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
        final String type;
        final String parentGuid;
        final boolean placeholder;
        final String description;

        AccountSeed(String name, String type, String parentGuid, boolean placeholder, String description) {
            this.name = name;
            this.type = type;
            this.parentGuid = parentGuid;
            this.placeholder = placeholder;
            this.description = description;
        }
    }
}
