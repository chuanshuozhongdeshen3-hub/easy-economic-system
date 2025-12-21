package com.moon.backend.service;

import com.moon.backend.dto.AuthResponse;
import com.moon.backend.dto.LoginRequest;
import com.moon.backend.dto.RegisterRequest;
import com.moon.backend.entity.SysUser;
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
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("用户名已存在");
        }

        SysUser sysUser = new SysUser();
        sysUser.setUsername(request.getUsername());
        sysUser.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        sysUser.setEnabled(true);
        sysUser.setCreatedAt(LocalDateTime.now());
        sysUser.setUpdatedAt(LocalDateTime.now());
        SysUser savedUser = userRepository.save(sysUser);

        createDefaultBookForUser(savedUser.getId());
        return new AuthResponse(savedUser.getId(), savedUser.getUsername());
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

        return new AuthResponse(user.getId(), user.getUsername());
    }

    private void createDefaultBookForUser(Long userId) {
        String bookGuid = UUID.randomUUID().toString();
        String rootAccountGuid = UUID.randomUUID().toString();
        LocalDateTime now = LocalDateTime.now();

        String disableForeignKeys = "SET FOREIGN_KEY_CHECKS=0";
        String enableForeignKeys = "SET FOREIGN_KEY_CHECKS=1";

        try {
            jdbcTemplate.execute(disableForeignKeys);
            jdbcTemplate.update(
                    "INSERT INTO books (guid, name, size, registered_capital_num, registered_capital_denom, root_account_guid, fiscal_year_start_month, fiscal_year_start_day, created_at, updated_at) " +
                            "VALUES (?, ?, NULL, NULL, NULL, ?, NULL, NULL, ?, ?)",
                    bookGuid,
                    "默认账本",
                    rootAccountGuid,
                    now,
                    now
            );

            jdbcTemplate.update(
                    "INSERT INTO accounts (guid, book_guid, name, code, description, account_type, parent_guid, hidden, placeholder, created_at, updated_at) " +
                            "VALUES (?, ?, ?, NULL, ?, ?, NULL, 0, 1, ?, ?)",
                    rootAccountGuid,
                    bookGuid,
                    "根账户",
                    "自动创建的根账户",
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
    }
}
