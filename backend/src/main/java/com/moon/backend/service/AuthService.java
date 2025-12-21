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
}
